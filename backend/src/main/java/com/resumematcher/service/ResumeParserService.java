package com.resumematcher.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Extracts plain text from an uploaded resume file (PDF or TXT).
 *
 * Why a separate service?
 *   The controller should not contain business logic. By isolating parsing
 *   here, we can unit-test it independently and swap the implementation
 *   (e.g., add DOCX support) without touching the controller.
 */
@Service
public class ResumeParserService {

    // 2 MB in bytes — resumes rarely exceed this; protects against abuse
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    /**
     * Entry point: validates the file, then delegates to the correct parser.
     *
     * @param file the uploaded MultipartFile from the HTTP request
     * @return the extracted text content of the resume
     * @throws IllegalArgumentException if validation fails (empty, too large, wrong type)
     * @throws RuntimeException         if reading the file fails (IO error, corrupt PDF)
     */
    public String extractText(MultipartFile file) {

        // ── Validation 1: file must not be null or empty ──
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "No file was uploaded. Please select a PDF or TXT file.");
        }

        // ── Validation 2: reject files larger than 2 MB ──
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File size exceeds 2 MB. Please upload a smaller resume.");
        }

        // ── Validation 3: only PDF and TXT are supported ──
        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new IllegalArgumentException("File name is missing.");
        }

        // Convert to lowercase so ".PDF" and ".pdf" both work
        String lowerName = originalName.toLowerCase();

        if (lowerName.endsWith(".pdf")) {
            return extractFromPdf(file);
        } else if (lowerName.endsWith(".txt")) {
            return extractFromTxt(file);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported file type. Only .pdf and .txt files are allowed.");
        }
    }

    // ── PDF extraction using Apache PDFBox ────────────────────────

    /**
     * How PDFBox works internally:
     *
     *   1. PDDocument.load(inputStream) parses the PDF's binary structure —
     *      a PDF is NOT plain text; it's a tree of objects (pages, fonts,
     *      streams) described in a cross-reference table.
     *
     *   2. PDFTextStripper walks each page's content stream. A content stream
     *      contains operators like "Tj" (show text) and "Td" (move cursor).
     *      The stripper collects every text-drawing operator, applies the
     *      font's character encoding to decode bytes into Unicode, and
     *      reconstructs the reading order based on glyph positions.
     *
     *   3. getText(document) returns all pages' text concatenated.
     *
     * Limitation: if the PDF is a scanned image (no text operators, only
     * embedded images), PDFTextStripper returns an empty string because
     * there is no text layer to read. OCR (e.g., Tesseract) would be needed.
     */
    private String extractFromPdf(MultipartFile file) {
        // try-with-resources ensures both the stream and the PDDocument
        // are closed even if an exception is thrown (prevents memory leaks)
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Guard against scanned PDFs that contain images but no text
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException(
                        "Could not extract text from the PDF. "
                        + "The file may be scanned/image-based. "
                        + "Please upload a text-based PDF or a .txt file.");
            }

            return text.trim();

        } catch (IOException e) {
            // Wrap in RuntimeException so the controller can catch it uniformly
            throw new RuntimeException(
                    "Failed to read the PDF file: " + e.getMessage(), e);
        }
    }

    // ── TXT extraction (plain file reading) ───────────────────────

    /**
     * Reads the entire file as a UTF-8 string.
     * MultipartFile.getBytes() loads the whole file into memory — acceptable
     * here because we already validated the size is ≤ 2 MB.
     */
    private String extractFromTxt(MultipartFile file) {
        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);

            if (text.isBlank()) {
                throw new IllegalArgumentException(
                        "The TXT file is empty. Please upload a file with content.");
            }

            return text.trim();

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read the TXT file: " + e.getMessage(), e);
        }
    }
}
