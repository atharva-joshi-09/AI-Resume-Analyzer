package com.example.resumeanalyzer.controller;

import com.example.resumeanalyzer.model.AnalysisResult;
import com.example.resumeanalyzer.model.ApiResponse;
import com.example.resumeanalyzer.service.ResumeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Single REST endpoint that powers the whole app: upload a resume,
 * get back a structured analysis. See {@link ApiResponse} for the
 * response envelope and {@link AnalysisResult} for its shape.
 */
@RestController
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping(value = "/api/analyze", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse> analyze(
            @RequestParam("resume") MultipartFile resume,
            @RequestParam(value = "jobDescription", required = false) String jobDescription) {

        AnalysisResult result = resumeService.analyzeResume(resume, jobDescription);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
