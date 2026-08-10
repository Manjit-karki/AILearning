package com.example.Backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "flashcards")
@CompoundIndex(name = "userId_documentId_idx", def = "{'userId': 1, 'documentId': 1}")
public class Flashcard {

    @Id
    private String id;

    private String userId;      // Store as String
    private String documentId;

    @Builder.Default
    private List<Card> cards=new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Card {
        private String question;
        private String answer;

        @Builder.Default
        private Difficulty difficulty = Difficulty.MEDIUM;

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        private LocalDateTime lastReviewed;

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @Builder.Default
        private Integer reviewCount = 0;

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @Builder.Default
        private Boolean isStarred = false;
    }
}
