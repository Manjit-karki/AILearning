
package com.example.Backend.repository;

import com.example.Backend.model.Quiz;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.data.mongodb.repository.Query;
import java.util.Optional;

@Repository
public interface QuizRepository extends MongoRepository<Quiz, String> {

    List<Quiz> findByUserId(String userId);

    List<Quiz> findByUserIdAndDocumentId(String userId, String documentId);

    Optional<Quiz> findByIdAndUserId(String id, String userId);

    //Incomplete quizzes
    List<Quiz> findByUserIdAndCompletedAtIsNull(String userId);

    //completed quizzes
    List<Quiz> findByUserIdAndCompletedAtIsNotNull(String userId);

    // Fetch only score and completedAt
    @Query(value = "{'userId': ?0, 'documentId': ?1}", fields = "{'score': 1, 'totalQuestions': 1, 'completedAt': 1}")
    List<Quiz> findScoresByUserIdAndDocumentId(String userId, String documentId);

    void deleteByUserIdAndDocumentId(String userId, String documentId);
}