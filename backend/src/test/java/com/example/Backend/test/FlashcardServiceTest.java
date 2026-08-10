package com.example.Backend.test;

import com.example.Backend.model.Flashcard;
import com.example.Backend.repository.FlashcardRepository;
import com.example.Backend.services.FlashcardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlashcardServiceTest {

    @Mock
    private FlashcardRepository flashcardRepository;

    @InjectMocks
    private FlashcardService flashcardService;

    private String userId;
    private String documentId;
    private Flashcard testFlashcard;
    private List<Flashcard.Card> testCards;

    @BeforeEach
    void setUp() {
        userId = "user123";
        documentId = "doc456";

        Flashcard.Card card1 = new Flashcard.Card();
        card1.setIsStarred(false);

        Flashcard.Card card2 = new Flashcard.Card();
        card2.setIsStarred(true);

        testCards = new ArrayList<>();
        testCards.add(card1);
        testCards.add(card2);

        testFlashcard = Flashcard.builder()
                .userId(userId)
                .documentId(documentId)
                .cards(testCards)
                .build();
    }

    @Test
    @DisplayName("createFlashcard - Should successfully save and return flashcard")
    void createFlashcard_Success() {
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(testFlashcard);

        Flashcard result = flashcardService.createFlashcard(userId, documentId, testCards);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(documentId, result.getDocumentId());
        assertEquals(2, result.getCards().size());
        verify(flashcardRepository, times(1)).save(any(Flashcard.class));
    }

    @Test
    @DisplayName("getByUserAndDocument - Should return flashcard when found")
    void getByUserAndDocument_Success() {
        when(flashcardRepository.findByUserIdAndDocumentId(userId, documentId))
                .thenReturn(Optional.of(testFlashcard));

        Flashcard result = flashcardService.getByUserAndDocument(userId, documentId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(flashcardRepository, times(1)).findByUserIdAndDocumentId(userId, documentId);
    }

    @Test
    @DisplayName("getByUserAndDocument - Should throw exception when not found")
    void getByUserAndDocument_NotFound_ThrowsException() {
        when(flashcardRepository.findByUserIdAndDocumentId(userId, documentId))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                flashcardService.getByUserAndDocument(userId, documentId)
        );

        assertEquals("Flashcard not found", exception.getMessage());
        verify(flashcardRepository, times(1)).findByUserIdAndDocumentId(userId, documentId);
    }

    @Test
    @DisplayName("getAllByUser - Should return list of flashcards")
    void getAllByUser_Success() {
        when(flashcardRepository.findByUserId(userId)).thenReturn(List.of(testFlashcard));

        List<Flashcard> results = flashcardService.getAllByUser(userId);

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(flashcardRepository, times(1)).findByUserId(userId);
    }

    @Test
    @DisplayName("updateCards - Should update cards and save")
    void updateCards_Success() {
        when(flashcardRepository.findByUserIdAndDocumentId(userId, documentId))
                .thenReturn(Optional.of(testFlashcard));
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(testFlashcard);

        List<Flashcard.Card> newCards = List.of(new Flashcard.Card());
        Flashcard result = flashcardService.updateCards(userId, documentId, newCards);

        assertNotNull(result);
        assertEquals(1, result.getCards().size());
        verify(flashcardRepository, times(1)).save(testFlashcard);
    }

    @Test
    @DisplayName("toggleStar - Should flip false to true for valid index")
    void toggleStar_FalseToTrue_Success() {
        when(flashcardRepository.findByUserIdAndDocumentId(userId, documentId))
                .thenReturn(Optional.of(testFlashcard));
        when(flashcardRepository.save(any(Flashcard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Index 0 has isStarred = false
        Flashcard result = flashcardService.toggleStar(userId, documentId, 0);

        assertTrue(result.getCards().get(0).getIsStarred());
        verify(flashcardRepository, times(1)).save(testFlashcard);
    }

    @Test
    @DisplayName("toggleStar - Should flip true to false for valid index")
    void toggleStar_TrueToFalse_Success() {
        when(flashcardRepository.findByUserIdAndDocumentId(userId, documentId))
                .thenReturn(Optional.of(testFlashcard));
        when(flashcardRepository.save(any(Flashcard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Index 1 has isStarred = true
        Flashcard result = flashcardService.toggleStar(userId, documentId, 1);

        assertFalse(result.getCards().get(1).getIsStarred());
        verify(flashcardRepository, times(1)).save(testFlashcard);
    }

    @Test
    @DisplayName("toggleStar - Should throw exception for negative index")
    void toggleStar_NegativeIndex_ThrowsException() {
        when(flashcardRepository.findByUserIdAndDocumentId(userId, documentId))
                .thenReturn(Optional.of(testFlashcard));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                flashcardService.toggleStar(userId, documentId, -1)
        );

        assertEquals("Invalid card index", exception.getMessage());
        verify(flashcardRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggleStar - Should throw exception for out-of-bounds index")
    void toggleStar_OutOfBoundsIndex_ThrowsException() {
        when(flashcardRepository.findByUserIdAndDocumentId(userId, documentId))
                .thenReturn(Optional.of(testFlashcard));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                flashcardService.toggleStar(userId, documentId, 99)
        );

        assertEquals("Invalid card index", exception.getMessage());
        verify(flashcardRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete - Should delegate deletion to repository")
    void delete_Success() {
        doNothing().when(flashcardRepository).deleteByUserIdAndDocumentId(userId, documentId);

        flashcardService.delete(userId, documentId);

        verify(flashcardRepository, times(1)).deleteByUserIdAndDocumentId(userId, documentId);
    }
}