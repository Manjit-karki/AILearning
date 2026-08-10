package com.example.Backend.services;

import com.example.Backend.model.ChatHistory;
import com.example.Backend.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ChatHistoryRepository chatHistoryRepository;
    private final MongoTemplate mongoTemplate;

    public ChatHistory createChatHistory(String userId, String documentId) {
        ChatHistory chatHistory = ChatHistory.builder()
                .userId(userId)
                .documentId(documentId)
                .build();
        return chatHistoryRepository.save(chatHistory);
    }

    public ChatHistory getByUserAndDocument(String userId, String documentId) {
        return chatHistoryRepository.findByUserIdAndDocumentId(userId, documentId)
                .orElseThrow(() -> new RuntimeException("Chat history not found"));
    }

    // Efficiently push a single message without loading entire document
    public void appendMessage(String userId, String documentId, ChatHistory.Message message) {
        Query query = new Query(
                Criteria.where("userId").is(userId)
                        .and("documentId").is(documentId)
        );
        Update update = new Update().push("messages", message);
        mongoTemplate.updateFirst(query, update, ChatHistory.class);
    }

    // Append multiple message
    public void appendMessages(String userId, String documentId, List<ChatHistory.Message> messages) {
        Query query = new Query(
                Criteria.where("userId").is(userId)
                        .and("documentId").is(documentId)
        );
        Update update = new Update().push("messages").each(messages);
        mongoTemplate.updateFirst(query, update, ChatHistory.class);
    }

    public void clearMessages(String userId, String documentId) {
        Query query = new Query(
                Criteria.where("userId").is(userId)
                        .and("documentId").is(documentId)
        );
        Update update = new Update().set("messages", List.of());
        mongoTemplate.updateFirst(query, update, ChatHistory.class);
    }

    public void delete(String userId, String documentId) {
        chatHistoryRepository.deleteByUserIdAndDocumentId(userId, documentId);
    }

    // Get pattern
    public ChatHistory getOrCreate(String userId, String documentId) {
        return chatHistoryRepository.findByUserIdAndDocumentId(userId, documentId)
                .orElseGet(() -> createChatHistory(userId, documentId));
    }
}
