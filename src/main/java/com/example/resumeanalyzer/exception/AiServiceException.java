package com.example.resumeanalyzer.exception;

/**
 * Thrown when the configured AI provider cannot be reached, returns an
 * error, or returns a response that cannot be parsed into our schema -
 * and no fallback could be used either.
 */
public class AiServiceException extends RuntimeException {
    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
