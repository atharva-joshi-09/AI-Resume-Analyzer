package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.config.AIConfig;
import com.example.resumeanalyzer.exception.AiServiceException;
import com.example.resumeanalyzer.model.AnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * The main use-case service: takes an uploaded resume (+ optional job
 * description) and produces a final {@link AnalysisResult}, coordinating
 * PDF extraction, the live AI call, and the demo fallback.
 */
@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    private final PdfExtractionService pdfExtractionService;
    private final AIAnalysisService aiAnalysisService;
    private final DemoAnalysisService demoAnalysisService;
    private final AIConfig aiConfig;

    public ResumeService(PdfExtractionService pdfExtractionService,
                          AIAnalysisService aiAnalysisService,
                          DemoAnalysisService demoAnalysisService,
                          AIConfig aiConfig) {
        this.pdfExtractionService = pdfExtractionService;
        this.aiAnalysisService = aiAnalysisService;
        this.demoAnalysisService = demoAnalysisService;
        this.aiConfig = aiConfig;
    }

    public AnalysisResult analyzeResume(MultipartFile resumeFile, String jobDescription) {
        PdfExtractionService.ExtractedText extracted = pdfExtractionService.extract(resumeFile);

        if (!aiConfig.isConfigured()) {
            log.info("AI_API_KEY not configured - using demo analysis mode.");
            return demoAnalysisService.analyze(extracted.fullText(), jobDescription);
        }

        try {
            return aiAnalysisService.analyze(extracted.textForAi(), jobDescription);
        } catch (AiServiceException e) {
            if (aiConfig.isDemoFallbackEnabled()) {
                log.warn("AI analysis failed ({}), falling back to demo mode.", e.getMessage());
                return demoAnalysisService.analyze(extracted.fullText(), jobDescription);
            }
            throw e;
        }
    }
}
