package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.model.AnalysisResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Produces a deterministic, rule-based analysis when the real AI provider
 * is unavailable (missing key, quota exceeded, network down, AI error).
 * <p>
 * This is clearly NOT presented as AI output - {@link AnalysisResult#isDemoMode()}
 * is set to true so the frontend can label it "Demo analysis mode".
 * It exists purely so a college demo never breaks because of an API outage.
 */
@Service
public class DemoAnalysisService {

    private static final List<String> KNOWN_SKILLS = Arrays.asList(
            "java", "python", "c++", "c", "javascript", "typescript", "spring boot", "spring",
            "react", "node.js", "sql", "mysql", "postgresql", "mongodb", "git", "github",
            "docker", "kubernetes", "aws", "azure", "gcp", "rest api", "restful", "junit",
            "html", "css", "linux", "data structures", "algorithms", "machine learning"
    );

    public AnalysisResult analyze(String resumeText, String jobDescription) {
        String lowerResume = resumeText.toLowerCase(Locale.ROOT);

        List<String> foundSkills = KNOWN_SKILLS.stream()
                .filter(lowerResume::contains)
                .map(this::titleCase)
                .toList();

        AnalysisResult result = new AnalysisResult();
        result.setDemoMode(true);
        result.setSkills(new ArrayList<>(foundSkills));

        int structureScore = scoreStructure(lowerResume);
        int skillScore = Math.min(100, foundSkills.size() * 10);
        int overall = (int) Math.round(structureScore * 0.5 + skillScore * 0.5);
        result.setOverallScore(overall);

        int atsScore = scoreAts(lowerResume);
        result.setAtsScore(atsScore);

        result.setSectionAnalysis(buildSectionAnalysis(lowerResume));

        if (jobDescription != null && !jobDescription.isBlank()) {
            applyJobMatch(result, lowerResume, jobDescription.toLowerCase(Locale.ROOT), foundSkills);
        }

        result.setStrengths(buildStrengths(foundSkills, lowerResume));
        result.setWeaknesses(buildWeaknesses(lowerResume, foundSkills));
        result.setSuggestions(buildSuggestions(lowerResume));

        result.setSummary("Demo analysis mode: this summary is generated with simple keyword and "
                + "structure checks (not a live AI call). The resume mentions " + foundSkills.size()
                + " recognizable technical skills and includes "
                + countPresentSections(lowerResume) + " of the common resume sections checked below.");

        return result;
    }

    private int scoreStructure(String lowerResume) {
        String[] sections = {"experience", "education", "project", "skill", "summary", "certification"};
        int found = 0;
        for (String s : sections) {
            if (lowerResume.contains(s)) {
                found++;
            }
        }
        return Math.min(100, (int) Math.round((found / (double) sections.length) * 100));
    }

    private int scoreAts(String lowerResume) {
        int score = 70;
        if (lowerResume.contains("table")) {
            score -= 5;
        }
        if (lowerResume.contains("@")) {
            score += 10;
        }
        if (lowerResume.contains("phone") || lowerResume.matches("(?s).*\\d{10}.*")) {
            score += 5;
        }
        if (lowerResume.length() < 500) {
            score -= 10;
        }
        return Math.max(0, Math.min(100, score));
    }

    private List<AnalysisResult.SectionAnalysis> buildSectionAnalysis(String lowerResume) {
        List<AnalysisResult.SectionAnalysis> sections = new ArrayList<>();
        sections.add(section(lowerResume, "Contact Information", new String[]{"@", "phone", "email"},
                "Includes an email address or phone number so recruiters can reach you.",
                "Add a clear email address and phone number near the top of the resume."));
        sections.add(section(lowerResume, "Professional Summary", new String[]{"summary", "objective"},
                "A short summary section is present.",
                "Consider adding a 2-3 line summary highlighting your focus area."));
        sections.add(section(lowerResume, "Education", new String[]{"education", "b.tech", "bachelor", "university", "college"},
                "Education details were found.",
                "Add your degree, institution, and expected graduation year."));
        sections.add(section(lowerResume, "Experience", new String[]{"experience", "internship", "worked"},
                "Experience or internship details were found.",
                "If you don't have formal experience yet, that's normal for students - lean on projects instead."));
        sections.add(section(lowerResume, "Projects", new String[]{"project"},
                "Project section detected.",
                "Add project descriptions with technologies used and measurable outcomes where possible."));
        sections.add(section(lowerResume, "Skills", new String[]{"skill"},
                "A skills section is present.",
                "List specific technologies rather than vague terms like \"good with computers\"."));
        sections.add(section(lowerResume, "Certifications", new String[]{"certification", "certified", "certificate"},
                "Certifications were found.",
                "Certifications are optional but can strengthen a resume if genuinely earned."));
        return sections;
    }

    private AnalysisResult.SectionAnalysis section(String lowerResume, String name, String[] markers,
                                                     String goodFeedback, String missingFeedback) {
        boolean present = Arrays.stream(markers).anyMatch(lowerResume::contains);
        return new AnalysisResult.SectionAnalysis(
                name,
                present ? "Found" : "Not Found",
                present ? 75 : 40,
                present ? goodFeedback : missingFeedback
        );
    }

    private int countPresentSections(String lowerResume) {
        return (int) buildSectionAnalysis(lowerResume).stream()
                .filter(s -> "Found".equals(s.getStatus()))
                .count();
    }

    private void applyJobMatch(AnalysisResult result, String lowerResume, String lowerJobDesc, List<String> foundSkills) {
        List<String> jobSkills = KNOWN_SKILLS.stream()
                .filter(lowerJobDesc::contains)
                .toList();

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String skill : jobSkills) {
            if (lowerResume.contains(skill)) {
                matched.add(titleCase(skill));
            } else {
                missing.add(titleCase(skill));
            }
        }

        result.setMatchedKeywords(matched);
        result.setMissingKeywords(missing);
        result.setMissingSkills(missing);

        int matchScore = jobSkills.isEmpty() ? 50
                : (int) Math.round((matched.size() / (double) jobSkills.size()) * 100);
        result.setJobMatchScore(matchScore);
    }

    private List<String> buildStrengths(List<String> foundSkills, String lowerResume) {
        List<String> strengths = new ArrayList<>();
        if (!foundSkills.isEmpty()) {
            strengths.add("Resume lists recognizable technical skills (" + foundSkills.size() + " detected).");
        }
        if (lowerResume.contains("project")) {
            strengths.add("Includes a projects section, which is valuable for students without much work experience.");
        }
        if (lowerResume.contains("education") || lowerResume.contains("b.tech")) {
            strengths.add("Education details are clearly present.");
        }
        if (lowerResume.contains("@")) {
            strengths.add("Contact information is easy to find.");
        }
        if (strengths.isEmpty()) {
            strengths.add("The resume was successfully parsed and contains readable text.");
        }
        return strengths;
    }

    private List<String> buildWeaknesses(String lowerResume, List<String> foundSkills) {
        List<String> weaknesses = new ArrayList<>();
        if (foundSkills.size() < 3) {
            weaknesses.add("Few recognizable technical skills were detected - consider listing them explicitly in a Skills section.");
        }
        if (!lowerResume.matches("(?s).*\\d+%.*") && !lowerResume.matches("(?s).*\\d+x.*")) {
            weaknesses.add("Project/experience descriptions could include more measurable outcomes (numbers, percentages).");
        }
        if (!lowerResume.contains("summary") && !lowerResume.contains("objective")) {
            weaknesses.add("No professional summary was detected near the top of the resume.");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("No major issues detected by the demo checks - a live AI review may still find nuances these simple checks miss.");
        }
        return weaknesses;
    }

    private List<String> buildSuggestions(String lowerResume) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("Add measurable results to project descriptions where genuinely applicable (e.g. performance improvements, users served).");
        suggestions.add("Use standard section headings (Education, Experience, Projects, Skills) so both humans and ATS parsers can find them.");
        if (!lowerResume.contains("github")) {
            suggestions.add("Consider linking a GitHub profile if you have public code you're proud of.");
        }
        suggestions.add("Consider learning and adding relevant skills to your resume only if you genuinely have experience with them.");
        return suggestions;
    }

    private String titleCase(String s) {
        if (s.isEmpty()) {
            return s;
        }
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
