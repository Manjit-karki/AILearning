package com.example.Backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.Backend.DTO.AiActionRequest;
import com.example.Backend.DTO.AiActionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {
    private static final Logger log = LoggerFactory.getLogger(AIController.class);

    private final ChatClient.Builder chatClientBuilder;
    private final VectorStore vectorStore;

    @PostMapping("/summarize")
    public ResponseEntity<AiActionResponse> summarize(@RequestBody AiActionRequest request,
                                                      @AuthenticationPrincipal UserDetails user ) {
        String prompt = """
                Summarize the following topic in clear, concise study notes.
                Use bullet points where helpful. Keep it exam-focused.
                Topic: %s
                """.formatted(request.query());

        String result = runWithContext(prompt, request);
        return ResponseEntity.ok(new AiActionResponse("summary", request.query(), result));
    }

    @PostMapping("/explain")
    public ResponseEntity<AiActionResponse> explain(@RequestBody AiActionRequest request,
                                                    @AuthenticationPrincipal UserDetails user) {
        String prompt = """
                Explain the following concept in a way a student can easily understand.
                Break it into simple steps or an analogy if useful. Include a brief example.
                Concept: %s
                """.formatted(request.query());

        String result = runWithContext(prompt, request);
        return ResponseEntity.ok(new AiActionResponse("explanation", request.query(), result));
    }

    private String runWithContext(String prompt, AiActionRequest request) {
        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder()
                .query(request.query())
                .topK(5)
                .similarityThreshold(0.5);

        if (request.documentId() != null && !request.documentId().isBlank()) {
            searchRequestBuilder.filterExpression(
                    "document_id == '" + request.documentId() + "'"
            );
        }

        QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequestBuilder.build())
                .build();

        log.info("Running AI action for query: {}", request.query());

        return chatClientBuilder.build()
                .prompt()
                .advisors(advisor)
                .user(prompt)
                .call()
                .content();
    }
}
