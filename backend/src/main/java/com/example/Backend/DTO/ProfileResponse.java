package com.example.Backend.DTO;

public record ProfileResponse(
        String userId,
        String name,
        String username,
        String email
) {}