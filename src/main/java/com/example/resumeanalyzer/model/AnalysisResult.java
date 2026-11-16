package com.example.resumeanalyzer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * The structured result of analyzing a resume (optionally against a job
 * description). This is the exact shape the AI is instructed to return as
 * JSON, and the exact shape the demo fallback produces, so the frontend
 * always receives a predictable schema regardless of source.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalysisResult {

    private Integer overallScore;
    private Integer atsScore;
    private Integer jobMatchScore; // null if no job description supplied

    private String summary;

    private List<String> skills = new ArrayList<>();
    private List<String> missingSkills = new ArrayList<>();

    private List<String> matchedKeywords = new ArrayList<>();
    private List<String> missingKeywords = new ArrayList<>();

    private List<String> strengths = new ArrayList<>();
    private List<String> weaknesses = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();

    private List<SectionAnalysis> sectionAnalysis = new ArrayList<>();

    /** True when this result came from the deterministic fallback, not a live LLM call. */
    private boolean demoMode = false;

    public AnalysisResult() {
    }

    // --- Getters / setters ---

    public Integer getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Integer overallScore) {
        this.overallScore = clamp(overallScore);
    }

    public Integer getAtsScore() {
        return atsScore;
    }

    public void setAtsScore(Integer atsScore) {
        this.atsScore = clamp(atsScore);
    }

    public Integer getJobMatchScore() {
        return jobMatchScore;
    }

    public void setJobMatchScore(Integer jobMatchScore) {
        this.jobMatchScore = jobMatchScore == null ? null : clamp(jobMatchScore);
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills == null ? new ArrayList<>() : skills;
    }

    public List<String> getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(List<String> missingSkills) {
        this.missingSkills = missingSkills == null ? new ArrayList<>() : missingSkills;
    }

    public List<String> getMatchedKeywords() {
        return matchedKeywords;
    }

    public void setMatchedKeywords(List<String> matchedKeywords) {
        this.matchedKeywords = matchedKeywords == null ? new ArrayList<>() : matchedKeywords;
    }

    public List<String> getMissingKeywords() {
        return missingKeywords;
    }

    public void setMissingKeywords(List<String> missingKeywords) {
        this.missingKeywords = missingKeywords == null ? new ArrayList<>() : missingKeywords;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths == null ? new ArrayList<>() : strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<String> weaknesses) {
        this.weaknesses = weaknesses == null ? new ArrayList<>() : weaknesses;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions == null ? new ArrayList<>() : suggestions;
    }

    public List<SectionAnalysis> getSectionAnalysis() {
        return sectionAnalysis;
    }

    public void setSectionAnalysis(List<SectionAnalysis> sectionAnalysis) {
        this.sectionAnalysis = sectionAnalysis == null ? new ArrayList<>() : sectionAnalysis;
    }

    public boolean isDemoMode() {
        return demoMode;
    }

    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
    }

    private static Integer clamp(Integer value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, value));
    }

    /**
     * Per-section feedback (e.g. "Experience", "Projects", "Education").
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SectionAnalysis {
        private String section;
        private String status; // "Found" | "Not Found" | "Good" | "Needs Improvement"
        private Integer score;
        private String feedback;

        public SectionAnalysis() {
        }

        public SectionAnalysis(String section, String status, Integer score, String feedback) {
            this.section = section;
            this.status = status;
            this.score = score;
            this.feedback = feedback;
        }

        public String getSection() {
            return section;
        }

        public void setSection(String section) {
            this.section = section;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public String getFeedback() {
            return feedback;
        }

        public void setFeedback(String feedback) {
            this.feedback = feedback;
        }
    }
}
