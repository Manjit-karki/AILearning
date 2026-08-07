package com.example.Backend.DTO;

public record AiActionRequest(
        String query,        // topic to summarize, or concept to explain
        String documentId    // optional, e.g. "Chemistry-XII-2077" — filters retrieval scope
) {}
