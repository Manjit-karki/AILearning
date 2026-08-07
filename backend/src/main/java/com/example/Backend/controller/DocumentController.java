package com.example.Backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private static final String CHEMISTRY_DOC_ID = "Chemistry-XII-2077";

    @Value("classpath:/documents/Chemistry-XII-2077-full-book.pdf")
    private Resource chemistryPdf;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> listAvailableDocuments(
            @AuthenticationPrincipal UserDetails user) {
        List<Map<String, String>> docs = List.of(
                Map.of("documentId", CHEMISTRY_DOC_ID, "title", "Chemistry XII (2077)")
        );
        return ResponseEntity.ok(ApiResponse.ok(docs, "Available documents fetched successfully"));
    }

    // Streams the raw PDF bytes for a document viewer (e.g. react-pdf, pdf.js, <iframe>, <embed>)
    @GetMapping("/{documentId}/view")
    public ResponseEntity<Resource> viewDocument(
            @PathVariable String documentId,
            @AuthenticationPrincipal UserDetails user) {
        try {
            if (!CHEMISTRY_DOC_ID.equals(documentId)) {
                return ResponseEntity.notFound().build();
            }

            if (!chemistryPdf.exists() || !chemistryPdf.isReadable()) {
                log.error("viewDocument: PDF resource not found or unreadable for documentId={}", documentId);
                return ResponseEntity.status(500).build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    // inline = render in-browser viewer, not force-download
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + chemistryPdf.getFilename() + "\"")
                    .contentLength(chemistryPdf.contentLength())
                    .body(chemistryPdf);
        } catch (IOException e) {
            log.error("viewDocument: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}