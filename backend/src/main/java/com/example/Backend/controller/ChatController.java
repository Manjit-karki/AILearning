package com.example.Backend.controller;

import com.example.Backend.model.ChatHistory;
import com.example.Backend.services.ChatHistoryService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatClient.Builder chatClientBuilder;
    private final VectorStore vectorStore;
    private final ChatHistoryService chatHistoryService;
    private static final FilterExpressionBuilder FILTER = new FilterExpressionBuilder();

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> postMessage(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails user) {
        try {
            String userId = userId(user);
            String documentId = request.documentId();
            String userMessage = request.message();

            if (userMessage == null || userMessage.isBlank())
                return bad400("message must not be empty");

            // Ensure a history record exists for this user+document pair
            chatHistoryService.getOrCreate(userId, documentId);

            // Build the vector store advisor, scoped to this document only
            QuestionAnswerAdvisor.Builder advisorBuilder = QuestionAnswerAdvisor.builder(vectorStore);
            if (documentId != null && !documentId.isBlank()) {
                Filter.Expression filter = FILTER.eq("documentId", documentId).build();
                advisorBuilder.searchRequest(SearchRequest.builder()
                        .filterExpression(filter)
                        .topK(4)
                        .build());
            }

            ChatClient chatClient = chatClientBuilder.build();
            String aiResponse = chatClient.prompt()
                    .advisors(advisorBuilder.build())
                    .user(userMessage)
                    .call()
                    .content();

            ChatHistory.Message userMsg = ChatHistory.Message.builder()
                    .role(ChatHistory.Message.Role.USER)
                    .content(userMessage)
                    .timestamp(LocalDateTime.now())
                    .build();
            ChatHistory.Message assistantMsg = ChatHistory.Message.builder()
                    .role(ChatHistory.Message.Role.AI)
                    .content(aiResponse)
                    .timestamp(LocalDateTime.now())
                    .build();
            chatHistoryService.appendMessages(userId, documentId, java.util.List.of(userMsg, assistantMsg));

            return ResponseEntity.ok(ApiResponse.ok(
                    Map.of("response", aiResponse),
                    "Message processed successfully"
            ));
        } catch (Exception e) {
            log.error("postMessage: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }

    @GetMapping("/{documentId}/history")
    public ResponseEntity<ApiResponse<ChatHistory>> getHistory(
            @PathVariable String documentId,
            @AuthenticationPrincipal UserDetails user) {
        try {
            ChatHistory history = chatHistoryService.getByUserAndDocument(userId(user), documentId);
            return ResponseEntity.ok(ApiResponse.ok(history, "Chat history fetched successfully"));
        } catch (RuntimeException e) {
            ChatHistory empty = ChatHistory.builder()
                    .userId(userId(user))
                    .documentId(documentId)
                    .build();
            return ResponseEntity.ok(ApiResponse.ok(empty, "No chat history yet"));
        }
    }

    @DeleteMapping("/{documentId}/history")
    public ResponseEntity<ApiResponse<Void>> clearHistory(
            @PathVariable String documentId,
            @AuthenticationPrincipal UserDetails user) {
        try {
            chatHistoryService.clearMessages(userId(user), documentId);
            return ResponseEntity.ok(ApiResponse.ok(null, "Chat history cleared successfully"));
        } catch (Exception e) {
            log.error("clearHistory: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }

    private String userId(UserDetails u) { return u.getUsername(); }

    private <T> ResponseEntity<ApiResponse<T>> bad400(String msg) {
        return ResponseEntity.status(400).body(ApiResponse.error(400, msg));
    }

    @Data
    public static class ChatRequest {
        @NotBlank(message = "message is required")
        private String message;
        private String documentId;

        public String message() { return message; }
        public String documentId() { return documentId; }
    }
}