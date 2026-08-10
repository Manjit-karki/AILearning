package com.example.Backend.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {


    private boolean success;
    private T data;
    private String message;
    private String error;
    private Integer statusCode;

    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .statusCode(200)
                .build();
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .statusCode(201)
                .build();
    }

    public static <T> ApiResponse<T> error(int statusCode, String error) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .statusCode(statusCode)
                .build();
    }
}
