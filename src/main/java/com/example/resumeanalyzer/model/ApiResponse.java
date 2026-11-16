package com.example.resumeanalyzer.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Envelope for every response the /api/analyze endpoint sends back.
 * Keeping success/error in one predictable shape makes the frontend
 * JS simple: check `success`, then read either `analysis` or `error`.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {

    private boolean success;
    private AnalysisResult analysis;
    private String error;

    public static ApiResponse ok(AnalysisResult analysis) {
        ApiResponse response = new ApiResponse();
        response.success = true;
        response.analysis = analysis;
        return response;
    }

    public static ApiResponse error(String message) {
        ApiResponse response = new ApiResponse();
        response.success = false;
        response.error = message;
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public AnalysisResult getAnalysis() {
        return analysis;
    }

    public String getError() {
        return error;
    }
}
