package com.example.Backend.ai;

import com.example.Backend.model.Flashcard;
import com.example.Backend.services.FlashcardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlashcardGenService {

    private static final Logger log = LoggerFactory.getLogger(FlashcardGenService.class);
    private static final int MAX_GENERATION_ATTEMPTS = 3;

    private final ChatClient chatClient;
    private final FlashcardService flashcardService;
    private final VectorStore vectorStore;

    public record FlashcardResponse(List<Flashcard.Card> cards) {}

    public FlashcardGenService(ChatClient.Builder chatClient,
                               FlashcardService flashcardService,
                               VectorStore vectorStore) {
        this.chatClient = chatClient.build();
        this.flashcardService = flashcardService;
        this.vectorStore = vectorStore;
    }

    public List<Flashcard.Card> generateCards(String context, int count) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            try {
                FlashcardResponse response = this.chatClient.prompt()
                        .user(u -> u.text("""
                                You are an educational assistant. Your ONLY source of information is the text
                                provided below. Do NOT use any outside knowledge, training data, or facts not
                                explicitly present in this text, even if you know them to be true.
                                
                                Extract key concepts from the text below and generate {count} flashcards.
                                If the text does not contain enough distinct concepts to generate {count}
                                flashcards, generate fewer rather than inventing content that isn't there.
                                
                                Text: "{context}"
                                
                                For each card item, provide:
                                - "question": A concise question, answerable using ONLY the text above.
                                - "answer": The correct answer, drawn ONLY from the text above.
                                - "difficulty": One of the following exact string values: "EASY", "MEDIUM", or "HARD".
                                
                                Respond with ONLY a valid JSON object with a single key "cards" containing the array of flashcard objects.
                                """)
                                .param("count", String.valueOf(count))
                                .param("context", context))
                        .call()
                        .entity(FlashcardResponse.class);

                if (response != null && response.cards() != null) {
                    return response.cards();
                }
            } catch (RuntimeException e) {
                lastFailure = e;
                log.warn("Flashcard generation attempt {}/{} failed to parse AI output: {}",
                        attempt, MAX_GENERATION_ATTEMPTS, e.getMessage());
            }
        }
        log.error("Flashcard generation failed after {} attempts", MAX_GENERATION_ATTEMPTS, lastFailure);
        throw new RuntimeException(
                "AI failed to generate valid flashcards after " + MAX_GENERATION_ATTEMPTS + " attempts. Please try again.",
                lastFailure);
    }

    public Flashcard generateAndSave(String userId, String documentId, String context, int count) {
        List<Flashcard.Card> cards = generateCards(context, count);
        return flashcardService.createFlashcard(userId, documentId, cards);
    }

    public Flashcard generateAndSaveFromVectorStore(String userId, String documentId, int count) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("key concepts and summaries")
                        .topK(4)
                        .build()
        );

        List<String> textChunks = docs.stream()
                .map(Document::getText)
                .collect(Collectors.toList());

        String context = String.join("\n\n", textChunks);

        if (context.isBlank()) {
            throw new RuntimeException(
                    "No ingested content found for document '" + documentId + "'. " +
                            "Make sure this document has been ingested into the vector store.");
        }

        return generateAndSave(userId, documentId, context, count);
    }
}