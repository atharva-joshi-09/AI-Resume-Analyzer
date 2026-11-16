package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.config.AIConfig;
import com.example.resumeanalyzer.exception.AiServiceException;
import com.example.resumeanalyzer.model.AnalysisResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AIAnalysisServiceTest {

    private final AIAnalysisService service = new AIAnalysisService(new AIConfig());

    @Test
    void parsesWellFormedAiJson() {
        String json = """
                {
                  "overallScore": 78,
                  "atsScore": 82,
                  "jobMatchScore": null,
                  "summary": "A solid resume.",
                  "skills": ["Java", "Spring Boot"],
                  "missingSkills": [],
                  "matchedKeywords": [],
                  "missingKeywords": [],
                  "strengths": ["Clear structure"],
                  "weaknesses": ["Needs more metrics"],
                  "suggestions": ["Add measurable results"],
                  "sectionAnalysis": [
                    {"section": "Projects", "status": "Found", "score": 80, "feedback": "Good."}
                  ]
                }
                """;

        AnalysisResult result = service.parseAndValidate(json);

        assertEquals(78, result.getOverallScore());
        assertEquals(82, result.getAtsScore());
        assertNull(result.getJobMatchScore());
        assertFalse(result.isDemoMode());
        assertEquals(2, result.getSkills().size());
    }

    @Test
    void parsesJsonWrappedInMarkdownFences() {
        String fenced = "```json\n{\"overallScore\": 60, \"atsScore\": 65}\n```";

        AnalysisResult result = service.parseAndValidate(fenced);

        assertEquals(60, result.getOverallScore());
        assertEquals(65, result.getAtsScore());
    }

    @Test
    void clampsOutOfRangeScoresTo0To100() {
        String json = "{\"overallScore\": 150, \"atsScore\": -20}";

        AnalysisResult result = service.parseAndValidate(json);

        assertEquals(100, result.getOverallScore());
        assertEquals(0, result.getAtsScore());
    }

    @Test
    void throwsAiServiceExceptionOnMalformedJson() {
        String malformed = "{ this is not valid json ";

        assertThrows(AiServiceException.class, () -> service.parseAndValidate(malformed));
    }

    @Test
    void throwsAiServiceExceptionWhenRequiredScoresMissing() {
        String missingScores = "{\"summary\": \"no scores here\"}";

        assertThrows(AiServiceException.class, () -> service.parseAndValidate(missingScores));
    }
}
