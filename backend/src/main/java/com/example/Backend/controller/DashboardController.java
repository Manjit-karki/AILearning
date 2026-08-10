package com.example.Backend.controller;

import com.example.Backend.model.SUser;
import com.example.Backend.repository.UserRepository;
import com.example.Backend.services.ProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ProgressService progressService;
    private final UserRepository userRepository;

    @GetMapping("/home")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHomeDashboard(
            @AuthenticationPrincipal UserDetails principal) {
        try {
            String username = principal.getUsername();
            SUser user = userRepository.findByUsernameOrEmail(username, username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> data = progressService.getDashboardData(user.getUserId(), username);
            return ResponseEntity.ok(ApiResponse.ok(data, "Dashboard fetched successfully"));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found"))
                return ResponseEntity.status(404).body(ApiResponse.error(404, "User not found"));
            log.error("getHomeDashboard: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }
}