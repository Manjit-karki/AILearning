package com.example.Backend.model;

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
@Document(collection = "chathistories")
@CompoundIndex(name = "userId_documentId_idx", def = "{'userId': 1, 'documentId': 1}",unique = true)
public class ChatHistory {

    @Id
    private String id;

    private String userId;
    private String documentId;

    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @CompoundIndex(name = "userId_documentId_idx", def = "{'userId': 1, 'documentId': 1}")
    public static class Message {
        private Role role;
        private String content;

        @Builder.Default
        private LocalDateTime timestamp = LocalDateTime.now();

        @Builder.Default
        private List<Integer> relevantChunks = new ArrayList<>();
        public enum Role {
            USER, AI
        }
    }
}
