
package com.example.Backend.controller;

import com.example.Backend.ai.QuizGenService;
import com.example.Backend.model.answer;
import com.example.Backend.model.questions;
import com.example.Backend.model.Quiz;
import com.example.Backend.services.QuizService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final QuizGenService quizGenService; // Injected AI service

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Quiz>> generateQuiz(
            @Valid @RequestBody GenerateQuizRequest req,
            @AuthenticationPrincipal UserDetails user) {
        try {
            Quiz quiz = quizGenService.generateAndSaveFromVectorStore(
                    userId(user),
                    req.getDocumentId(),
                    req.getTitle(),
                    req.getTopic(),
                    req.getCount()
            );
            return ResponseEntity.ok(ApiResponse.ok(quiz, "Quiz generated successfully"));
        } catch (Exception e) {
            log.error("generateQuiz: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Quiz>>> getAllQuizzes(
            @AuthenticationPrincipal UserDetails user) {
        try {
            List<Quiz> quizzes = quizService.getAllByUser(userId(user));
            return ResponseEntity.ok(ApiResponse.ok(quizzes, "Quizzes fetched successfully"));
        } catch (Exception e) {
            log.error("getAllQuizzes: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }

    @GetMapping("/document/{documentId}")
    public ResponseEntity<ApiResponse<List<Quiz>>> getQuizzesByDocument(
            @PathVariable String documentId,
            @AuthenticationPrincipal UserDetails user) {
        try {
            List<Quiz> quizzes = quizService.getByUserAndDocument(userId(user), documentId);
            return ResponseEntity.ok(ApiResponse.ok(quizzes, "Quizzes fetched successfully"));
        } catch (Exception e) {
            log.error("getQuizzesByDocument: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Quiz>> getQuizById(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails user) {
        try {
            Quiz quiz = quizService.getById(id, userId(user));
            return ResponseEntity.ok(ApiResponse.ok(quiz, "Quiz fetched successfully"));
        } catch (RuntimeException e) {
            if (notFound(e)) return notFound404("Quiz not found");
            log.error("getQuizById: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQuizResults(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails user) {
        try {
            Quiz quiz = quizService.getById(id, userId(user));
            List<answer> userAnswers = quiz.getUserAnswers() != null ? quiz.getUserAnswers() : Collections.emptyList();

            List<Map<String, Object>> questionResults = quiz.getQuestions().stream()
                    .map(q -> {
                        int idx = quiz.getQuestions().indexOf(q);
                        answer userAnswer = userAnswers.stream()
                                .filter(a -> a.getQuestionIndex() == idx)
                                .findFirst().orElse(null);

                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("questionIndex",  idx);
                        r.put("questionText",    q.getQuestion());
                        r.put("options",         q.getOptions());
                        r.put("correctAnswer",   q.getCorrectAnswer());
                        r.put("explanation",     q.getExplanation());
                        r.put("difficulty",      q.getDifficulty());
                        r.put("selectedAnswer",  userAnswer != null ? userAnswer.getSelectedAnswer() : null);
                        r.put("isCorrect",       userAnswer != null ? userAnswer.getIsCorrect()      : null);
                        r.put("isAnswered",      userAnswer != null);
                        return r;
                    }).toList();

            int percentage = quiz.getTotalQuestions() > 0
                    ? Math.round((float) quiz.getScore() / quiz.getTotalQuestions() * 100) : 0;

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("quizId",          quiz.getId());
            data.put("title",           quiz.getTitle());
            data.put("documentId",      quiz.getDocumentId());
            data.put("score",           quiz.getScore());
            data.put("totalQuestions",  quiz.getTotalQuestions());
            data.put("totalAnswered",   userAnswers.size());
            data.put("percentage",      percentage);
            data.put("isCompleted",     quiz.getCompletedAt() != null);
            data.put("completedAt",     quiz.getCompletedAt());
            data.put("questions",       questionResults);

            return ResponseEntity.ok(ApiResponse.ok(data, "Quiz results fetched successfully"));
        } catch (RuntimeException e) {
            if (notFound(e)) return notFound404("Quiz not found");
            log.error("getQuizResults: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }

    @PostMapping("/{id}/answer")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitAnswer(
            @PathVariable String id,
            @Valid @RequestBody SubmitAnswerRequest req,
            @AuthenticationPrincipal UserDetails user) {
        try {
            String userId = userId(user);
            Quiz quiz = quizService.getById(id, userId);

            if (quiz.getCompletedAt() != null)
                return bad400("Quiz has already been completed");

            int idx = req.getQuestionIndex();
            if (idx < 0 || idx >= quiz.getQuestions().size())
                return bad400("questionIndex must be between 0 and " + (quiz.getQuestions().size() - 1));

            List<answer> existingAnswers = quiz.getUserAnswers() != null ? quiz.getUserAnswers() : Collections.emptyList();
            boolean alreadyAnswered = existingAnswers.stream()
                    .anyMatch(a -> a.getQuestionIndex() == idx);
            if (alreadyAnswered)
                return bad400("Question " + idx + " has already been answered");

            questions question = quiz.getQuestions().get(idx);
            boolean isCorrect = req.getSelectedAnswer().trim().equalsIgnoreCase(question.getCorrectAnswer().trim());

            answer answer = com.example.Backend.model.answer.builder()
                    .questionIndex(idx)
                    .selectedAnswer(req.getSelectedAnswer())
                    .isCorrect(isCorrect)
                    .answeredAt(LocalDateTime.now())
                    .build();

            quizService.submitAnswer(id, answer);

            int newTotalAnswered = existingAnswers.size() + 1;
            boolean isCompleted  = newTotalAnswered == quiz.getTotalQuestions();
            if (isCompleted) quizService.completeQuiz(id, userId);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("isCorrect",      isCorrect);
            data.put("correctAnswer",  question.getCorrectAnswer());
            data.put("explanation",    question.getExplanation());
            data.put("currentScore",   quiz.getScore() + (isCorrect ? 1 : 0));
            data.put("totalAnswered",  newTotalAnswered);
            data.put("totalQuestions", quiz.getTotalQuestions());
            data.put("isCompleted",    isCompleted);

            return ResponseEntity.ok(
                    ApiResponse.ok(data, isCorrect ? "Correct answer!" : "Incorrect answer"));
        } catch (RuntimeException e) {
            if (notFound(e)) return notFound404("Quiz not found");
            log.error("submitAnswer: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }

    @PostMapping("/{id}/reset")
    public ResponseEntity<ApiResponse<Void>> resetQuiz(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails user) {
        try {
            quizService.resetQuiz(id, userId(user));
            return ResponseEntity.ok(ApiResponse.ok(null, "Quiz reset successfully"));
        } catch (RuntimeException e) {
            if (notFound(e)) return notFound404("Quiz not found");
            log.error("resetQuiz: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQuiz(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails user) {
        try {
            quizService.delete(id, userId(user));
            return ResponseEntity.ok(ApiResponse.ok(null, "Quiz deleted successfully"));
        } catch (RuntimeException e) {
            if (notFound(e)) return notFound404("Quiz not found");
            log.error("deleteQuiz: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }

    @Data
    static class GenerateQuizRequest {
        @NotBlank(message = "documentId is required")
        private String documentId;

        @NotBlank(message = "title is required")
        private String title;

        @NotBlank(message = "topic is required")
        private String topic;

        @Min(value = 1, message = "count must be at least 1")
        private int count = 5;
    }

    @Data
    static class SubmitAnswerRequest {
        @NotNull(message = "questionIndex is required")
        private Integer questionIndex;

        @NotBlank(message = "selectedAnswer is required")
        private String selectedAnswer;
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
}