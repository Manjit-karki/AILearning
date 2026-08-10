package com.example.Backend.test;

import com.example.Backend.ai.DocumentIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * NOTE: PagePdfDocumentReader is instantiated with `new` inside run(), which Mockito
 * can't normally intercept via a regular mock/stub. We use Mockito's mockConstruction
 * (requires mockito-inline, included by default in spring-boot-starter-test) to
 * intercept that construction and control what .read() returns.
 */
@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private Resource resource;

    private DocumentIngestionService service;

    @BeforeEach
    void setUp() {
        service = new DocumentIngestionService(vectorStore);
        ReflectionTestUtils.setField(service, "resource", resource);
    }

    @Test
    void run_skipsIngestionWhenDocumentAlreadyExists() {
        Document existing = new Document("already ingested chunk");
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(existing));

        service.run();

        // Should never attempt to write to the vector store if the skip-check found a match
        verify(vectorStore, never()).accept(any());
    }
}