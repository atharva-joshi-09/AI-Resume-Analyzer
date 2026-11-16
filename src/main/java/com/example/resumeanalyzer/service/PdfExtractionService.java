package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.exception.ResumeValidationException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Extracts plain text from an uploaded PDF resume using Apache PDFBox.
 * Validates file type, size, and extractable content before passing text to downstream services.
 */
@Service
public class PdfExtractionService {

    private static final Logger log = LoggerFactory.getLogger(PdfExtractionService.class);

    /** Minimum number of non-whitespace characters we consider "meaningful text". */
    private static final int MIN_MEANINGFUL_CHARS = 50;

    /** Cap on characters sent onward to the AI, to keep prompts small and cheap. */
    private static final int MAX_CHARS_FOR_AI = 12_000;

    public ExtractedText extract(MultipartFile file) {
        validateFile(file);

        String rawText;
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            if (document.isEncrypted()) {
                throw new ResumeValidationException(
                        "This PDF is password-protected. Please upload an unprotected PDF resume.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            rawText = stripper.getText(document);
        } catch (ResumeValidationException e) {
            throw e;
        } catch (IOException e) {
            log.warn("Failed to parse uploaded PDF: {}", e.getMessage());
            throw new ResumeValidationException(
                    "We couldn't read this PDF. Please make sure it is a valid, non-corrupted PDF file.");
        }

        String cleaned = cleanText(rawText);

        long meaningfulChars = cleaned.chars().filter(c -> !Character.isWhitespace(c)).count();
        if (meaningfulChars < MIN_MEANINGFUL_CHARS) {
            throw new ResumeValidationException(
                    "This resume appears to be scanned or image-based. Please upload a text-based PDF.");
        }

        boolean truncated = false;
        String forAi = cleaned;
        if (forAi.length() > MAX_CHARS_FOR_AI) {
            forAi = forAi.substring(0, MAX_CHARS_FOR_AI);
            truncated = true;
        }

        return new ExtractedText(cleaned, forAi, truncated);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResumeValidationException("Please upload a resume file.");
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();

        boolean looksLikePdf = "application/pdf".equals(contentType) || filename.endsWith(".pdf");
        if (!looksLikePdf) {
            throw new ResumeValidationException("Please upload a PDF resume.");
        }

        // Extra safety net in addition to the server-level multipart size limit.
        long maxBytes = 5L * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new ResumeValidationException("The uploaded file is larger than the allowed limit (5 MB).");
        }
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        // Collapse excessive blank lines/whitespace while preserving structure.
        return text.replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    /**
     * @param fullText     the full cleaned resume text (used for local keyword matching)
     * @param textForAi    the (possibly truncated) text actually sent to the LLM
     * @param wasTruncated whether textForAi was shortened from fullText
     */
    public record ExtractedText(String fullText, String textForAi, boolean wasTruncated) {
    }
}
