package com.example.Backend.test;

import com.example.Backend.ai.QuizGenService;
import com.example.Backend.model.Difficulty;
import com.example.Backend.model.Quiz;
import com.example.Backend.model.questions;
import com.example.Backend.services.QuizService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizGenServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private QuizService quizService;

    private ChatClient chatClient;
    private QuizGenService quizGenService;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        quizGenService = new QuizGenService(vectorStore, chatClientBuilder, quizService);
    }

    private List<questions> sampleQuestions() {
        return List.of(
                questions.builder()
                        .question("What is H2O?")
                        .options(List.of("Water", "Salt", "Sugar", "Oil"))
                        .correctAnswer("Water")
                        .explanation("H2O is water.")
                        .difficulty(Difficulty.EASY)
                        .build()
        );
    }

    @Test
    void generateQuestionsFromContext_succeedsOnFirstAttempt() {
        List<questions> expected = sampleQuestions();
        when(chatClient.prompt().user(any(Consumer.class)).call().entity(any(ParameterizedTypeReference.class)))
                .thenReturn(expected);

        List<questions> result = quizGenService.generateQuestionsFromContext("some context", 1);

        assertEquals(expected, result);
        verify(chatClient, times(1)).prompt();
    }

    @Test
    void generateQuestionsFromContext_retriesOnMalformedJsonThenSucceeds() {
        List<questions> expected = sampleQuestions();
        when(chatClient.prompt().user(any(Consumer.class)).call().entity(any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Unexpected character: was expecting comma"))
                .thenReturn(expected);

        List<questions> result = quizGenService.generateQuestionsFromContext("some context", 1);

        assertEquals(expected, result);
        verify(chatClient, times(2)).prompt();
    }

    @Test
    void generateQuestionsFromContext_throwsCleanErrorAfterAllRetriesExhausted() {
        when(chatClient.prompt().user(any(Consumer.class)).call().entity(any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("bad json 1"))
                .thenThrow(new RuntimeException("bad json 2"))
                .thenThrow(new RuntimeException("bad json 3"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> quizGenService.generateQuestionsFromContext("some context", 1));

        assertTrue(ex.getMessage().contains("AI failed to generate a valid quiz after 3 attempts"));
        verify(chatClient, times(3)).prompt();
    }

    @Test
    void generateAndSave_delegatesToQuizServiceWithGeneratedQuestions() {
        List<questions> qs = sampleQuestions();
        when(chatClient.prompt().user(any(Consumer.class)).call().entity(any(ParameterizedTypeReference.class)))
                .thenReturn(qs);

        Quiz expectedQuiz = Quiz.builder()
                .userId("user-1").documentId("doc-1").title("Chem Quiz").questions(qs).build();
        when(quizService.createQuiz("user-1", "doc-1", "Chem Quiz", qs)).thenReturn(expectedQuiz);

        Quiz result = quizGenService.generateAndSave("user-1", "doc-1", "Chem Quiz", "context", 1);

        assertEquals(expectedQuiz, result);
        verify(quizService).createQuiz("user-1", "doc-1", "Chem Quiz", qs);
    }

    @Test
    void generateAndSaveFromVectorStore_joinsRetrievedChunksIntoContext() {
        Document chunk1 = new Document("Chunk one text");
        Document chunk2 = new Document("Chunk two text");
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(chunk1, chunk2));

        List<questions> qs = sampleQuestions();
        when(chatClient.prompt().user(any(Consumer.class)).call().entity(any(ParameterizedTypeReference.class)))
                .thenReturn(qs);

        Quiz expectedQuiz = Quiz.builder().userId("user-1").documentId("doc-1").title("t").questions(qs).build();
        when(quizService.createQuiz(eq("user-1"), eq("doc-1"), eq("t"), eq(qs))).thenReturn(expectedQuiz);

        Quiz result = quizGenService.generateAndSaveFromVectorStore("user-1", "doc-1", "t", "topic", 1);

        assertEquals(expectedQuiz, result);
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void generateAndSaveFromVectorStore_fallsBackToTopicWhenRetrievalIsEmpty() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        List<questions> qs = sampleQuestions();
        when(chatClient.prompt().user(any(Consumer.class)).call().entity(any(ParameterizedTypeReference.class)))
                .thenReturn(qs);

        Quiz expectedQuiz = Quiz.builder().userId("u").documentId("d").title("t").questions(qs).build();
        when(quizService.createQuiz(eq("u"), eq("d"), eq("t"), eq(qs))).thenReturn(expectedQuiz);

        // No exception should be thrown even though retrieval returned nothing —
        // the service should fall back to using the raw topic string as context.
        Quiz result = quizGenService.generateAndSaveFromVectorStore("u", "d", "t", "some topic", 1);

        assertEquals(expectedQuiz, result);
    }
}