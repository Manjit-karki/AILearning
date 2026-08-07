package com.example.Backend.services;

import com.example.Backend.model.Flashcard;
import com.example.Backend.repository.FlashcardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlashcardService {

    private final FlashcardRepository flashcardRepository;

    public Flashcard createFlashcard(String userId, String documentId, List<Flashcard.Card> cards) {
        Flashcard flashcard = Flashcard.builder()
                .userId(userId)
                .documentId(documentId)
                .cards(cards)
                .build();
        return flashcardRepository.save(flashcard);
    }

    public Flashcard getByUserAndDocument(String userId, String documentId) {
        return flashcardRepository.findByUserIdAndDocumentId(userId, documentId)
                .orElseThrow(() -> new RuntimeException("Flashcard not found"));
    }

    public List<Flashcard> getAllByUser(String userId) {
        return flashcardRepository.findByUserId(userId);
    }

    public Flashcard updateCards(String userId, String documentId, List<Flashcard.Card> cards) {
        Flashcard flashcard = getByUserAndDocument(userId, documentId);
        flashcard.setCards(cards);
        return flashcardRepository.save(flashcard);
    }
    //trying to create toggle
    public Flashcard toggleStar(String userId, String documentId, int cardIndex) {
        Flashcard flashcard = getByUserAndDocument(userId, documentId);
        List<Flashcard.Card> cards = flashcard.getCards();

        if (cardIndex < 0||cardIndex >= cards.size()) {
            throw new RuntimeException("Invalid card index");
        }
        Flashcard.Card card = cards.get(cardIndex);
        boolean newStarred=!Boolean.TRUE.equals(card.getIsStarred());
        card.setIsStarred(newStarred);
        return flashcardRepository.save(flashcard);
    }

    public void delete(String userId, String documentId) {
        flashcardRepository.deleteByUserIdAndDocumentId(userId, documentId);
    }
}