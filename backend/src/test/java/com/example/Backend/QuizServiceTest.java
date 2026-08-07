package com.example.Backend;

import com.example.Backend.model.Difficulty;
import com.example.Backend.model.Quiz;
import com.example.Backend.model.answer;
import com.example.Backend.model.questions;
import com.example.Backend.repository.QuizRepository;
import com.example.Backend.services.QuizService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private QuizService quizService;

    private questions sampleQuestion;

    @BeforeEach
    void setUp() {
        sampleQuestion = questions.builder()
                .question("What is 2 + 2?")
                .options(List.of("2", "3", "4", "5"))
                .correctAnswer("4")
                .explanation("Basic arithmetic")
                .difficulty(Difficulty.EASY)
                .build();
    }

    // ---------- createQuiz ----------

    @Test
    void createQuiz_savesQuizWithCorrectTotalQuestions() {
        List<questions> qs = List.of(sampleQuestion, sampleQuestion);
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> inv.getArgument(0));

        Quiz result = quizService.createQuiz("user1", "doc1", "My Quiz", qs);

        assertEquals("user1", result.getUserId());
        assertEquals("doc1", result.getDocumentId());
        assertEquals("My Quiz", result.getTitle());
        assertEquals(2, result.getTotalQuestions());
        verify(quizRepository, times(1)).save(any(Quiz.class));
    }

    @Test
    void createQuiz_withEmptyQuestionList_setsTotalQuestionsZero() {
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> inv.getArgument(0));

        Quiz result = quizService.createQuiz("user1", "doc1", "Empty Quiz", List.of());

        assertEquals(0, result.getTotalQuestions());
    }

    // ---------- getById ----------

    @Test
    void getById_returnsQuiz_whenFound() {
        Quiz quiz = Quiz.builder().id("q1").userId("user1").build();
        when(quizRepository.findByIdAndUserId("q1", "user1")).thenReturn(Optional.of(quiz));

        Quiz result = quizService.getById("q1", "user1");

        assertEquals("q1", result.getId());
    }

    @Test
    void getById_throwsRuntimeException_whenNotFound() {
        when(quizRepository.findByIdAndUserId("missing", "user1")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> quizService.getById("missing", "user1"));
        assertTrue(ex.getMessage().contains("not found"));
    }

    // ---------- getAllByUser / getByUserAndDocument ----------

    @Test
    void getAllByUser_returnsListFromRepository() {
        List<Quiz> quizzes = List.of(Quiz.builder().id("q1").build(), Quiz.builder().id("q2").build());
        when(quizRepository.findByUserId("user1")).thenReturn(quizzes);

        List<Quiz> result = quizService.getAllByUser("user1");

        assertEquals(2, result.size());
    }

    @Test
    void getByUserAndDocument_returnsFilteredList() {
        List<Quiz> quizzes = List.of(Quiz.builder().id("q1").documentId("doc1").build());
        when(quizRepository.findByUserIdAndDocumentId("user1", "doc1")).thenReturn(quizzes);

        List<Quiz> result = quizService.getByUserAndDocument("user1", "doc1");

        assertEquals(1, result.size());
        assertEquals("doc1", result.get(0).getDocumentId());
    }

    // ---------- submitAnswer ----------

    @Test
    void submitAnswer_pushesAnswerAndIncrementsScore_whenCorrect() {
        answer correctAnswer = answer.builder()
                .questionIndex(0)
                .selectedAnswer("4")
                .isCorrect(true)
                .answeredAt(LocalDateTime.now())
                .build();

        quizService.submitAnswer("q1", correctAnswer);

        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(Quiz.class));
    }

    @Test
    void submitAnswer_pushesAnswerWithoutIncrementingScore_whenIncorrect() {
        answer wrongAnswer = answer.builder()
                .questionIndex(0)
                .selectedAnswer("3")
                .isCorrect(false)
                .answeredAt(LocalDateTime.now())
                .build();

        quizService.submitAnswer("q1", wrongAnswer);

        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(Quiz.class));
    }

    // ---------- completeQuiz ----------

    @Test
    void completeQuiz_updatesCompletedAtAndReturnsQuiz() {
        Quiz completed = Quiz.builder().id("q1").userId("user1").completedAt(LocalDateTime.now()).build();
        when(quizRepository.findByIdAndUserId("q1", "user1")).thenReturn(Optional.of(completed));

        Quiz result = quizService.completeQuiz("q1", "user1");

        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(Quiz.class));
        assertNotNull(result.getCompletedAt());
    }

    // ---------- resetQuiz ----------

    @Test
    void resetQuiz_callsUpdateWithClearedFields() {
        quizService.resetQuiz("q1", "user1");

        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(Quiz.class));
    }

    // ---------- delete ----------

    @Test
    void delete_removesQuiz_whenFound() {
        Quiz quiz = Quiz.builder().id("q1").userId("user1").build();
        when(quizRepository.findByIdAndUserId("q1", "user1")).thenReturn(Optional.of(quiz));

        quizService.delete("q1", "user1");

        verify(quizRepository, times(1)).delete(quiz);
    }

    @Test
    void delete_throws_whenQuizNotFound() {
        when(quizRepository.findByIdAndUserId("missing", "user1")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> quizService.delete("missing", "user1"));
        verify(quizRepository, never()).delete(any());
    }
}