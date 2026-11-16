package com.example.resumeanalyzer.controller;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * No AI_API_KEY is configured in the test environment, so these requests
 * exercise the demo fallback path end-to-end - useful because it doesn't
 * require network access or a real API key to verify the endpoint works.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void analyzeReturnsSuccessfulResultForValidPdf() throws Exception {
        byte[] pdf = buildPdfWithText("Java developer resume with Spring Boot, SQL and Git experience. "
                + "Education: B.Tech. Experience: Backend intern. Projects: REST API service.");

        MockMultipartFile file = new MockMultipartFile("resume", "resume.pdf", "application/pdf", pdf);

        mockMvc.perform(multipart("/api/analyze").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.analysis.overallScore").exists())
                .andExpect(jsonPath("$.analysis.atsScore").exists())
                .andExpect(jsonPath("$.analysis.demoMode").value(true));
    }

    @Test
    void analyzeRejectsNonPdfFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("resume", "resume.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/analyze").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void analyzeRejectsMissingFile() throws Exception {
        mockMvc.perform(multipart("/api/analyze"))
                .andExpect(status().is4xxClientError());
    }

    private byte[] buildPdfWithText(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                stream.newLineAtOffset(50, 700);
                stream.showText(text);
                stream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
