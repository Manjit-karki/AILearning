package com.example.Backend.ai;

import com.example.Backend.model.Flashcard;
import com.example.Backend.services.FlashcardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlashcardGenService {

    private static final Logger log = LoggerFactory.getLogger(FlashcardGenService.class);
    private static final int MAX_GENERATION_ATTEMPTS = 3;

    private final ChatClient chatClient;
    private final FlashcardService flashcardService;

    public FlashcardGenService(ChatClient.Builder chatClient, FlashcardService flashcardService) {
        this.chatClient = chatClient.build();
        this.flashcardService = flashcardService;
    }

    public List<Flashcard.Card> generateCards(String context, int count) {
        // Retry on malformed LLM JSON output (see QuizGenService for the same pattern) —
        // structured output occasionally comes back with a missing comma/unescaped quote,
        // which makes .entity()'s Jackson parse throw on the first attempt.
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            try {
                return this.chatClient.prompt()
                        .user(u -> u.text("""
                                 You are an educational assistant. Your ONLY source of information is the text
                                provided below. Do NOT use any outside knowledge, training data, or facts not
                                explicitly present in this text, even if you know them to be true.
                            
                                Extract key concepts from the text below and generate {count} flashcards.
                                If the text does not contain enough distinct concepts to generate {count}
                                flashcards, generate fewer rather than inventing content that isn't there.
 
                                Text: "{context}"
                                 For each item in the array, provide:
                                 - "question": A concise question, answerable using ONLY the text above.
                                 - "answer": The correct answer, drawn ONLY from the text above.
                                 - "difficulty": One of the following exact string values: "EASY", "MEDIUM", or "HARD".
                                
                                 Respond with ONLY a single valid JSON array. No markdown code fences, no commentary.
                                 Every string value must be properly quoted and every array/object entry must be
                                 separated by a comma. Escape any double quotes that appear inside a string value.
                                """)
                                .param("count", String.valueOf(count))
                                .param("context", context))
                        .call()
                        .entity(new ParameterizedTypeReference<List<Flashcard.Card>>() {});
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
}