package com.example.Backend.repository;

import com.example.Backend.model.Flashcard;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlashcardRepository extends MongoRepository<Flashcard, String> {

    Optional<Flashcard> findByUserIdAndDocumentId(String userId, String documentId);

    List<Flashcard> findByUserId(String userId);

    boolean existsByUserIdAndDocumentId(String userId, String documentId);

    void deleteByUserIdAndDocumentId(String userId, String documentId);
}