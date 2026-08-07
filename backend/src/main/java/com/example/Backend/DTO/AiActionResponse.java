package com.example.Backend.DTO;

public record AiActionResponse (
        String action,
        String query,
        String result
) {}
