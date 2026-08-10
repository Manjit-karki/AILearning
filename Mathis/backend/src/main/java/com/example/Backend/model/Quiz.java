package com.example.Backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "quiz")
public class Quiz {
    @Id
    private String id;
    @NotNull
    private String userId;
    @NotNull
    private String documentId;
    @NotBlank
    private String title;
    @Builder.Default
    private List<questions> questions=new ArrayList<>();
    @Builder.Default
    private List<answer> userAnswers = new ArrayList<>();
    @Builder.Default
    private Integer score = 0;

    @NotNull
    private Integer totalQuestions;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Builder.Default
    private LocalDateTime completedAt = null;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @CreatedDate
    private LocalDateTime createdAt;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @LastModifiedDate
    private LocalDateTime updatedAt;

}

