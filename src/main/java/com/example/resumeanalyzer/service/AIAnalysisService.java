package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.config.AIConfig;
import com.example.resumeanalyzer.exception.AiServiceException;
import com.example.resumeanalyzer.model.AnalysisResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Calls the configured LLM provider (Google Gemini by default) to analyze
 * a resume and returns a validated {@link AnalysisResult}.
 * <p>
 * The provider is intentionally isolated behind this one class - swapping
 * to OpenAI or another provider means changing {@link #callGemini(String)}
 * (or adding a sibling method) without touching the rest of the app.
 */
@Service
public class AIAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AIAnalysisService.class);

    private static final String SYSTEM_PROMPT = """
            You are an expert resume reviewer and ATS-oriented career assistant.

            Analyze the supplied resume objectively.
            Do not invent information. Do not assume the candidate has a skill unless the
            resume clearly indicates it. If a job description is provided, compare the
            resume against it and factor job relevance into the scores.

            Evaluate: resume structure, skills, projects, experience, education,
            achievements, ATS compatibility, keyword relevance, clarity, and
            quantifiable achievements.

            Scoring rubric for overallScore (0-100): content quality 25%, skills
            relevance 20%, projects/experience 20%, resume structure 15%, ATS
            compatibility 10%, clarity and language 10%. atsScore should separately
            reflect how parser-friendly the formatting and section structure are.

            Use constructive, encouraging language - never insulting.
            Never recommend fabricating experience, skills, certifications,
            employment, projects, or achievements. When a skill is not present in
            the resume, describe it as "not clearly mentioned" rather than claiming
            the candidate definitely lacks it.

            Return ONLY valid JSON matching exactly this schema, with no markdown
            fences and no extra commentary:

            {
              "overallScore": <integer 0-100>,
              "atsScore": <integer 0-100>,
              "jobMatchScore": <integer 0-100 or null if no job description was given>,
              "summary": <string, 2-4 sentences>,
              "skills": [<string>, ...],
              "missingSkills": [<string>, ...],
              "matchedKeywords": [<string>, ...],
              "missingKeywords": [<string>, ...],
              "strengths": [<string>, ... 3 to 7 items],
              "weaknesses": [<string>, ... 3 to 7 items],
              "suggestions": [<string>, ...],
              "sectionAnalysis": [
                {"section": <string>, "status": "Found"|"Not Found", "score": <integer 0-100>, "feedback": <string>}
              ]
            }
            """;

    private final AIConfig aiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile HttpClient httpClient;

    public AIAnalysisService(AIConfig aiConfig) {
        this.aiConfig = aiConfig;
    }

    private HttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(10))
                            .build();
                }
            }
        }
        return httpClient;
    }

    public AnalysisResult analyze(String resumeText, String jobDescription) {
        String userPrompt = buildUserPrompt(resumeText, jobDescription);
        String rawJson = callGemini(userPrompt);
        return parseAndValidate(rawJson);
    }

    private String buildUserPrompt(String resumeText, String jobDescription) {
        StringBuilder sb = new StringBuilder();
        sb.append("RESUME TEXT:\n---\n").append(resumeText).append("\n---\n\n");
        if (jobDescription != null && !jobDescription.isBlank()) {
            sb.append("TARGET JOB DESCRIPTION:\n---\n").append(jobDescription).append("\n---\n");
        } else {
            sb.append("No job description was provided. Set jobMatchScore to null and leave "
                    + "matchedKeywords/missingKeywords empty.\n");
        }
        return sb.toString();
    }

    private String callGemini(String userPrompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + aiConfig.getModel() + ":generateContent?key=" + aiConfig.getApiKey();

        Map<String, Object> body = Map.of(
                "system_instruction", Map.of("parts", new Object[]{Map.of("text", SYSTEM_PROMPT)}),
                "contents", new Object[]{Map.of("role", "user", "parts", new Object[]{Map.of("text", userPrompt)})},
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.4
                )
        );

        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new AiServiceException("Failed to build AI request.", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(aiConfig.getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response;
        try {
            response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.warn("Could not reach the AI provider: {}", e.getMessage(), e);
            throw new AiServiceException("Could not reach the AI provider: " + e.getMessage(), e);
        }

        if (response.statusCode() != 200) {
            log.warn("AI provider returned status {}: {}", response.statusCode(), truncate(response.body(), 300));
            throw new AiServiceException("AI provider returned an error (HTTP " + response.statusCode() + ").");
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                throw new AiServiceException("AI provider returned an empty response.");
            }
            return textNode.asText();
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("Could not parse the AI provider's response envelope.", e);
        }
    }

    // Package-private (not private) so unit tests can exercise JSON parsing/validation
    // directly without needing a live network call to the AI provider.
    AnalysisResult parseAndValidate(String rawJson) {
        String cleaned = stripMarkdownFences(rawJson);
        AnalysisResult result;
        try {
            result = objectMapper.readValue(cleaned, AnalysisResult.class);
        } catch (Exception e) {
            log.warn("Failed to parse AI JSON output: {}", truncate(cleaned, 300));
            throw new AiServiceException("The AI returned a response we couldn't understand.", e);
        }

        if (result.getOverallScore() == null || result.getAtsScore() == null) {
            throw new AiServiceException("The AI response was missing required score fields.");
        }
        result.setDemoMode(false);
        return result;
    }

    private String stripMarkdownFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(json)?", "").trim();
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
