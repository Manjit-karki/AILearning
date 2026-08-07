package com.example.Backend.controller;

import com.example.Backend.ai.FlashcardGenService;
import com.example.Backend.model.Flashcard;
import com.example.Backend.services.FlashcardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/flashcards")
@RequiredArgsConstructor
public class FlashcardController {
    private final FlashcardGenService flashcardGenService;
    private final FlashcardService flashcardService;


    @GetMapping
    public ResponseEntity<ApiResponse<List<Flashcard>>> getAllSets(
            @AuthenticationPrincipal UserDetails user) {
        try {
            List<Flashcard> sets = flashcardService.getAllByUser(userId(user));
            return ResponseEntity.ok(ApiResponse.ok(sets, "Flashcard sets fetched successfully"));
        } catch (Exception e) {
            log.error("getAllSets: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }


    @GetMapping("/document/{documentId}")
    public ResponseEntity<ApiResponse<Flashcard>> getByDocument(
            @PathVariable String documentId,
            @AuthenticationPrincipal UserDetails user) {
        try {
            Flashcard set = flashcardService.getByUserAndDocument(userId(user), documentId);
            return ResponseEntity.ok(ApiResponse.ok(set, "Flashcard set fetched successfully"));
        } catch (RuntimeException e) {
            if (notFound(e)) return notFound404("Flashcard set not found");
            log.error("getByDocument: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }


    @PostMapping("/document/{documentId}/generate")
    public ResponseEntity<ApiResponse<Flashcard>> generateAndSaveFlashcards(
            @PathVariable String documentId,
            @RequestBody GenerateRequest request,
            @RequestParam(defaultValue = "5") int count,
            @AuthenticationPrincipal UserDetails user) {
        try {
            String uid = userId(user);

            // 1. Generate cars
            List<Flashcard.Card> generatedCards = flashcardGenService.generateCards(request.sourceText(), count);

            if (generatedCards == null || generatedCards.isEmpty()) {
                return ResponseEntity.status(500)
                        .body(ApiResponse.error(500, "AI failed to generate flashcards or returned invalid format."));
            }

            // 2. Save directly to MongoDB
            Flashcard savedFlashcard = flashcardService.createFlashcard(uid, documentId, generatedCards);

            // 3. Return your standard API Response
            return ResponseEntity.ok(ApiResponse.ok(savedFlashcard, "Flashcards generated successfully"));

        } catch (Exception e) {
            log.error("generateAndSaveFlashcards: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }


    @PutMapping("/document/{documentId}/cards/{cardIndex}/star")
    public ResponseEntity<ApiResponse<Flashcard>> toggleStar(
            @PathVariable String documentId,
            @PathVariable int cardIndex,
            @AuthenticationPrincipal UserDetails user) {
        try {
            String uid = userId(user);
            Flashcard set = flashcardService.getByUserAndDocument(uid, documentId);

            if (cardIndex < 0 || cardIndex >= set.getCards().size())
                return bad400("cardIndex out of range");

            boolean willBeStarred = !Boolean.TRUE.equals(
                    set.getCards().get(cardIndex).getIsStarred());

            Flashcard updated = flashcardService.toggleStar(uid, documentId, cardIndex);
            String msg = willBeStarred ? "Card starred successfully" : "Card unstarred successfully";
            return ResponseEntity.ok(ApiResponse.ok(updated, msg));
        } catch (RuntimeException e) {
            if (notFound(e)) return notFound404("Flashcard set not found");
            log.error("toggleStar: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }

    @DeleteMapping("/document/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteSet(
            @PathVariable String documentId,
            @AuthenticationPrincipal UserDetails user) {
        try {
            String uid = userId(user);
            // Verify it exists first (so we return 404 instead of silently doing nothing)
            flashcardService.getByUserAndDocument(uid, documentId);
            flashcardService.delete(uid, documentId);
            return ResponseEntity.ok(ApiResponse.ok(null, "Flashcard set deleted successfully"));
        } catch (RuntimeException e) {
            if (notFound(e)) return notFound404("Flashcard set not found");
            log.error("deleteSet: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }

    private String userId(UserDetails u) { return u.getUsername(); }

    private boolean notFound(RuntimeException e) {
        return e.getMessage() != null && e.getMessage().contains("not found");
    }

    private <T> ResponseEntity<ApiResponse<T>> notFound404(String msg) {
        return ResponseEntity.status(404).body(ApiResponse.error(404, msg));
    }

    private <T> ResponseEntity<ApiResponse<T>> bad400(String msg) {
        return ResponseEntity.status(400).body(ApiResponse.error(400, msg));
    }
    public record GenerateRequest(String sourceText) {}
}