package com.example.Backend.ai;

import com.example.Backend.model.Flashcard;
import com.example.Backend.services.FlashcardService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlashcardGenService {

    private final ChatClient chatClient;
    private final FlashcardService flashcardService;

    public FlashcardGenService(ChatClient.Builder chatClient, FlashcardService flashcardService) {
        this.chatClient = chatClient.build();
        this.flashcardService = flashcardService;
    }

    public List<Flashcard.Card> generateCards(String context, int count) {

            return this.chatClient.prompt()
                    .user(u -> u.text("""
                            You are an educational assistant. Extract key concepts from the text below and generate {count} flashcards.
                            
                            Text: "{context}"
                            
                            For each item in the array, provide:
                            - "question": A concise question.
                            - "answer": The correct answer.
                            - "difficulty": One of the following exact string values: "EASY", "MEDIUM", or "HARD".
                            """)
                            .param("count", String.valueOf(count))
                            .param("context", context))
                    .call()
                    .entity(new ParameterizedTypeReference<List<Flashcard.Card>>() {});
                    }
    //Saves the genreated flashcard
    public Flashcard generateAndSave(String userId, String documentId, String context, int count) {
        List<Flashcard.Card> cards = generateCards(context, count);
        return flashcardService.createFlashcard(userId, documentId, cards);
    }
}