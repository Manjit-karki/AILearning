package com.example.Backend.services;

import com.example.Backend.model.answer;
import com.example.Backend.model.Quiz;
import com.example.Backend.model.questions;
import com.example.Backend.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService
{

    private final QuizRepository quizRepository;

    private final MongoTemplate mongoTemplate;


    //create
    public Quiz createQuiz(String userId, String documentId,
                           String title, List<questions> question) {
        Quiz quiz = Quiz.builder()
                .userId(userId)
                .documentId(documentId)
                .title(title)
                .questions(question)
                .totalQuestions(question.size())
                .build();
        return quizRepository.save(quiz);
    }
    //read
    public Quiz getById(String quizId, String userId) {
        return quizRepository.findByIdAndUserId(quizId, userId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
    }
    public List<Quiz> getAllByUser(String userId) {
        return quizRepository.findByUserId(userId);
    }

    public List<Quiz> getByUserAndDocument(String userId, String documentId) {
        return quizRepository.findByUserIdAndDocumentId(userId, documentId);
    }
    //submitting
    public void submitAnswer(String quizId, answer answer) {
        Query query = new Query(Criteria.where("id").is(quizId));
        Update update = new Update().push("userAnswers", answer);

        if (answer.getIsCorrect()) {
            update.inc("score", 1);         // atomic increment
        }
        mongoTemplate.updateFirst(query, update, Quiz.class);
    }
    //marking
    public Quiz completeQuiz(String quizId, String userId) {
        Query query = new Query(
                Criteria.where("id").is(quizId)
                        .and("userId").is(userId)
        );
        Update update = new Update().set("completedAt", LocalDateTime.now());
        mongoTemplate.updateFirst(query, update, Quiz.class);
        return getById(quizId, userId);
    }
    //reseting for next sessions
    public void resetQuiz(String quizId, String userId) {
        Query query = new Query(
                Criteria.where("id").is(quizId)
                        .and("userId").is(userId)
        );
        Update update = new Update()
                .set("userAnswers", List.of())
                .set("score", 0)
                .set("completedAt", null);
        mongoTemplate.updateFirst(query, update, Quiz.class);
    }
    //deleting quiz
    public void delete(String quizId, String userId) {
        Quiz quiz = getById(quizId, userId);
        quizRepository.delete(quiz);
    }
}
