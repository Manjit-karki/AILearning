package com.example.Backend.repository;

import com.example.Backend.model.ChatHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatHistoryRepository extends MongoRepository<ChatHistory, String> {

    Optional<ChatHistory> findByUserIdAndDocumentId(String userId, String documentId);

    List<ChatHistory> findByUserId(String userId);

    boolean existsByUserIdAndDocumentId(String userId, String documentId);

    void deleteByUserIdAndDocumentId(String userId, String documentId);

    // Fetch only messages field
    @Query(value = "{'userId': ?0, 'documentId': ?1}", fields = "{'messages': 1}")
    Optional<ChatHistory> findMessagesByUserIdAndDocumentId(String userId, String documentId);
}