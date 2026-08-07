package com.example.Backend.services;

import com.example.Backend.DTO.ProfileResponse;
import com.example.Backend.model.SUser;
import com.example.Backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileResponse getProfile(String username) {
        SUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toResponse(user);
    }

    public ProfileResponse updateProfile(String username, String newName, String newEmail) {
        SUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (newName != null && !newName.isBlank()) {
            user.setName(newName);
        }

        if (newEmail != null && !newEmail.isBlank() && !newEmail.equals(user.getEmail())) {
            if (userRepository.existsByEmail(newEmail)) {
                throw new RuntimeException("Email already in use");
            }
            user.setEmail(newEmail);
        }

        SUser saved = userRepository.save(user);
        return toResponse(saved);
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        SUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters long");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private ProfileResponse toResponse(SUser user) {
        return new ProfileResponse(user.getUserId(), user.getName(), user.getUsername(), user.getEmail());
    }
}
