package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.exception.ResumeValidationException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PdfExtractionServiceTest {

    private final PdfExtractionService service = new PdfExtractionService();

    @Test
    void extractsTextFromAValidPdf() throws IOException {
        byte[] pdfBytes = buildPdfWithText(
                "Rahul Sharma\nEducation: B.Tech Computer Science\nExperience: Backend intern\n"
                        + "Projects: Built a REST API using Spring Boot and MySQL.\nSkills: Java, Spring Boot, SQL, Git");

        MockMultipartFile file = new MockMultipartFile(
                "resume", "resume.pdf", "application/pdf", pdfBytes);

        PdfExtractionService.ExtractedText result = service.extract(file);

        assertTrue(result.fullText().contains("Rahul Sharma"));
        assertTrue(result.fullText().contains("Spring Boot"));
        assertFalse(result.wasTruncated());
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "resume", "empty.pdf", "application/pdf", new byte[0]);

        assertThrows(ResumeValidationException.class, () -> service.extract(file));
    }

    @Test
    void rejectsNonPdfFile() {
        MockMultipartFile file = new MockMultipartFile(
                "resume", "resume.txt", "text/plain", "Hello world".getBytes());

        ResumeValidationException ex = assertThrows(ResumeValidationException.class,
                () -> service.extract(file));
        assertTrue(ex.getMessage().toLowerCase().contains("pdf"));
    }

    @Test
    void rejectsOversizedFile() {
        byte[] big = new byte[6 * 1024 * 1024]; // 6 MB, over the 5 MB limit
        MockMultipartFile file = new MockMultipartFile(
                "resume", "big.pdf", "application/pdf", big);

        ResumeValidationException ex = assertThrows(ResumeValidationException.class,
                () -> service.extract(file));
        assertTrue(ex.getMessage().toLowerCase().contains("larger"));
    }

    @Test
    void rejectsPdfWithNoMeaningfulText() throws IOException {
        // A blank PDF page has no extractable text - simulates a scanned/image-only resume.
        byte[] blankPdf = buildBlankPdf();
        MockMultipartFile file = new MockMultipartFile(
                "resume", "scanned.pdf", "application/pdf", blankPdf);

        ResumeValidationException ex = assertThrows(ResumeValidationException.class,
                () -> service.extract(file));
        assertTrue(ex.getMessage().toLowerCase().contains("scanned"));
    }

    private byte[] buildPdfWithText(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                stream.newLineAtOffset(50, 700);
                for (String line : text.split("\n")) {
                    stream.showText(line);
                    stream.newLineAtOffset(0, -15);
                }
                stream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] buildBlankPdf() throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
