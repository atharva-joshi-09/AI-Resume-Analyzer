package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.model.AnalysisResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DemoAnalysisServiceTest {

    private final DemoAnalysisService service = new DemoAnalysisService();

    private static final String SAMPLE_RESUME = """
            Rahul Sharma
            rahul.sharma@example.com

            Education
            B.Tech Computer Science, XYZ University

            Experience
            Backend Development Intern

            Projects
            Built a REST API using Java, Spring Boot, MySQL and Git.

            Skills
            Java, Spring Boot, SQL, Git, REST API
            """;

    @Test
    void marksResultAsDemoMode() {
        AnalysisResult result = service.analyze(SAMPLE_RESUME, null);
        assertTrue(result.isDemoMode());
    }

    @Test
    void scoresStayWithinValidRange() {
        AnalysisResult result = service.analyze(SAMPLE_RESUME, null);
        assertTrue(result.getOverallScore() >= 0 && result.getOverallScore() <= 100);
        assertTrue(result.getAtsScore() >= 0 && result.getAtsScore() <= 100);
    }

    @Test
    void detectsSkillsMentionedInResume() {
        AnalysisResult result = service.analyze(SAMPLE_RESUME, null);
        assertTrue(result.getSkills().stream().anyMatch(s -> s.equalsIgnoreCase("Java")));
        assertTrue(result.getSkills().stream().anyMatch(s -> s.equalsIgnoreCase("Spring Boot")));
    }

    @Test
    void leavesJobMatchScoreNullWhenNoJobDescriptionGiven() {
        AnalysisResult result = service.analyze(SAMPLE_RESUME, null);
        assertNull(result.getJobMatchScore());
    }

    @Test
    void computesJobMatchWhenJobDescriptionGiven() {
        String jobDescription = "Looking for a Java developer with Docker and AWS experience.";
        AnalysisResult result = service.analyze(SAMPLE_RESUME, jobDescription);

        assertNotNull(result.getJobMatchScore());
        assertTrue(result.getMatchedKeywords().stream().anyMatch(s -> s.equalsIgnoreCase("Java")));
        assertTrue(result.getMissingKeywords().stream().anyMatch(s -> s.equalsIgnoreCase("Docker")));
    }

    @Test
    void producesNonEmptySectionAnalysis() {
        AnalysisResult result = service.analyze(SAMPLE_RESUME, null);
        assertFalse(result.getSectionAnalysis().isEmpty());
    }
}
