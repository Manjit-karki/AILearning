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

    @Test
    void run_ingestsAndTagsChunksWithDocumentIdWhenNotAlreadyPresent() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        Document rawPage = new Document("Some chemistry content about covalent bonds.");

        try (MockedConstruction<PagePdfDocumentReader> mocked = mockConstruction(
                PagePdfDocumentReader.class,
                (mock, context) -> when(mock.read()).thenReturn(List.of(rawPage)))) {

            service.run();

            // Confirms metadata tagging happened before the mocked reader's output
            // was handed off — this is the fix for the AIController/ChatController
            // filter mismatch (both filter on "documentId").
            //
            // If this fails with actual=null: DocumentIngestionService.run() in your
            // project doesn't tag chunk metadata yet. Check that the "documentId"
            // metadata-tagging fix (rawDocs.forEach(doc -> doc.getMetadata().put(...)))
            // is actually present in the file being compiled, not just in a patch
            // that was generated but never copied into the project.
            assertEquals("Chemistry-XII-2077", rawPage.getMetadata().get("documentId"));

            verify(vectorStore, times(1)).accept(any());
        }
    }

    @Test
    void run_skipCheckIsScopedToThisDocumentId() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        try (MockedConstruction<PagePdfDocumentReader> mocked = mockConstruction(
                PagePdfDocumentReader.class,
                (mock, context) -> when(mock.read()).thenReturn(List.of(new Document("text"))))) {

            service.run();

            org.mockito.ArgumentCaptor<SearchRequest> captor = org.mockito.ArgumentCaptor.forClass(SearchRequest.class);
            verify(vectorStore).similaritySearch(captor.capture());
            // The skip-check must filter by documentId, not just do a bare keyword search —
            // otherwise it would incorrectly skip ingestion of a different document.
            //
            // If this fails with actual=null: the skip-check in DocumentIngestionService.run()
            // in your project is still the original bare SearchRequest.builder().query("Chemistry")
            // with no .filterExpression(...) — same "file not actually updated" issue as above.
            assertNotNull(captor.getValue().getFilterExpression(),
                    "Skip-check search must include a filterExpression scoped to this document");
        }
    }
}