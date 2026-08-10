package com.example.Backend.controller;

import com.example.Backend.DTO.ProfileResponse;
import com.example.Backend.services.ProfileService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @AuthenticationPrincipal UserDetails user) {
        try {
            ProfileResponse profile = profileService.getProfile(userId(user));
            return ResponseEntity.ok(ApiResponse.ok(profile, "Profile fetched successfully"));
        } catch (RuntimeException e) {
            if (notFound(e)) return notFound404("User not found");
            log.error("getProfile: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails user) {
        try {
            ProfileResponse updated = profileService.updateProfile(
                    userId(user), request.getName(), request.getEmail());
            return ResponseEntity.ok(ApiResponse.ok(updated, "Profile updated successfully"));
        } catch (RuntimeException e) {
            if (notFound(e)) return notFound404("User not found");
            if (e.getMessage() != null && e.getMessage().contains("already in use"))
                return bad400(e.getMessage());
            log.error("updateProfile: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails user) {
        try {
            profileService.changePassword(
                    userId(user), request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.ok(null, "Password changed successfully"));
        } catch (RuntimeException e) {
            if (notFound(e)) return notFound404("User not found");
            if (e.getMessage() != null &&
                    (e.getMessage().contains("incorrect") || e.getMessage().contains("at least")))
                return bad400(e.getMessage());
            log.error("changePassword: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }

    private String userId(UserDetails u) { return u.getUsername(); }

    private boolean notFound(RuntimeException e) {
        return e.getMessage() != null && e.getMessage().contains("not found");
    }

    private <T> ResponseEntity<ApiResponse<T>> notFound404(String msg) {
        return ResponseEntity.status(404).body(ApiResponse.error(404, msg));
    }

    private <T> ResponseEntity<ApiResponse<T>> bad400(String msg) {
        return ResponseEntity.status(400).body(ApiResponse.error(400, msg));
    }

    @Data
    static class UpdateProfileRequest {
        private String name;
        @Email(message = "Please enter a valid email address")
        private String email;
    }

    @Data
    static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;
        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "New password must be at least 6 characters long")
        private String newPassword;
    }
}