package com.example.resumeanalyzer.exception;

/**
 * Thrown when the uploaded resume (or request) fails validation -
 * wrong file type, too large, empty, unreadable, etc. Always carries
 * a message that is safe and friendly to show directly to the user.
 */
public class ResumeValidationException extends RuntimeException {
    public ResumeValidationException(String message) {
        super(message);
    }
}
