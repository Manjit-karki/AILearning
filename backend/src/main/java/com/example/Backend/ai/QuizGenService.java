package com.example.Backend.ai;

import com.example.Backend.model.Quiz;
import com.example.Backend.model.questions;
import com.example.Backend.services.QuizService;
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
                        """)
                        .param("count", String.valueOf(count))
                        .param("context", context))
                .call()
                .entity(new ParameterizedTypeReference<List<questions>>() {});
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