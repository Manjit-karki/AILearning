package com.example.Backend.ai;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentIngestionService implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);
    private static final String DOCUMENT_ID = "Chemistry-XII-2077";

    @Value("classpath:/documents/Chemistry-XII-2077-full-book.pdf")
    private Resource resource;

    private final VectorStore vectorStore;

    @Override
    public void run(String... args) {
        List<Document> existingDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("Chemistry")
                        .topK(1)
                        .similarityThreshold(0.0)
                        .build()
        );

        if (!existingDocs.isEmpty()) {
            log.info("Vector store already populated. Skipping PDF ingestion.");
            return;
        }

        log.info("Processing PDF file...");
        PagePdfDocumentReader pdfDocumentReader = new PagePdfDocumentReader(resource);
        TextSplitter textSplitter = TokenTextSplitter.builder().build();

        // Split PDF pages into chunks
        List<Document> splitDocs = textSplitter.split(pdfDocumentReader.read());

        // Attach documentId to each chunk's metadata map
        splitDocs.forEach(doc -> doc.getMetadata().put("documentId", DOCUMENT_ID));

        vectorStore.accept(splitDocs);
        log.info("Successfully ingested {} chunks for documentId: {}", splitDocs.size(), DOCUMENT_ID);
    }
}