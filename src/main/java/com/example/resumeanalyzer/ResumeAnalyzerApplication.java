package com.example.resumeanalyzer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the AI Resume Analyzer application.
 * <p>
 * The app starts normally even if the AI API key is missing -
 * a configuration error is only shown when the user actually
 * tries to run an analysis. See {@link com.example.resumeanalyzer.config.AIConfig}.
 */
@SpringBootApplication
public class ResumeAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumeAnalyzerApplication.class, args);
    }
}
