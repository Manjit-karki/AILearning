package com.example.Backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class answer {
    @NotNull
    private Integer questionIndex;
    @NotNull
    private Boolean isCorrect;
    @NotBlank
    private String selectedAnswer;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Builder.Default
    private LocalDateTime answeredAt = LocalDateTime.now();
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Builder.Default
    private Difficulty difficulty = Difficulty.MEDIUM;
}

