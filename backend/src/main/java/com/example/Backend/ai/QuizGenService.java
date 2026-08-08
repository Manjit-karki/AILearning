package com.example.Backend.ai;

import com.example.Backend.model.Quiz;
import com.example.Backend.model.questions;
import com.example.Backend.services.QuizService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuizGenService {

    private static final Logger log = LoggerFactory.getLogger(QuizGenService.class);
    private static final int MAX_GENERATION_ATTEMPTS = 3;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final QuizService quizService;

    public QuizGenService(VectorStore vectorStore, ChatClient.Builder chatClientBuilder, QuizService quizService) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
        this.quizService = quizService;
    }

    //Generates questions using Spring AI given a raw context string.
    public List<questions> generateQuestionsFromContext(String context, int count) {
        // LLM structured-JSON output is non-deterministic and occasionally malformed
        // (e.g. a missing comma inside "options"), which makes Spring AI's .entity()
        // Jackson parse throw. Retry a few times before giving up, rather than failing
        // the whole request on the first bad generation.
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            try {
                return this.chatClient.prompt()
                        .user(u -> u.text("""
                                You are an educational assessment assistant.
                                Generate {count} multiple-choice quiz questions based on the text below.
                                
                                Text: "{context}"
                                
                                For each item, map to these exact fields:
                                - "question": The clear question statement.
                                - "options": A list of multiple-choice options.
                                - "correctAnswer": The correct answer choice string.
                                - "explanation" : A brief explanation of why the correct answer is right.
                                - "difficulty": One of the following exact string values: "EASY", "MEDIUM", or "HARD".
                                
                                Respond with ONLY a single valid JSON array. No markdown code fences, no commentary.
                                Every string value must be properly quoted and every array/object entry must be
                                separated by a comma. Escape any double quotes that appear inside a string value.
                                """)
                                .param("count", String.valueOf(count))
                                .param("context", context))
                        .call()
                        .entity(new ParameterizedTypeReference<List<questions>>() {});
            } catch (RuntimeException e) {
                lastFailure = e;
                log.warn("Quiz generation attempt {}/{} failed to parse AI output: {}",
                        attempt, MAX_GENERATION_ATTEMPTS, e.getMessage());
            }
        }
        log.error("Quiz generation failed after {} attempts", MAX_GENERATION_ATTEMPTS, lastFailure);
        throw new RuntimeException(
                "AI failed to generate a valid quiz after " + MAX_GENERATION_ATTEMPTS + " attempts. Please try again.",
                lastFailure);
    }
    //Generates questions from raw context and saves via QuizService.
    public Quiz generateAndSave(String userId, String documentId, String title, String context, int count) {
        List<questions> generatedQuestions = generateQuestionsFromContext(context, count);
        return quizService.createQuiz(userId, documentId, title, generatedQuestions);
    }
    //  Fetches relevant chunks from VectorStore before generating and saving.
    public Quiz generateAndSaveFromVectorStore(String userId, String documentId, String title, String topic, int count) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(topic)
                        .topK(4)
                        .build()
        );

        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        if (context.isBlank()) {
            context = topic;
        }

        return generateAndSave(userId, documentId, title, context, count);
    }
}