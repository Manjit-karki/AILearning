package com.example.Backend.controller;

import com.example.Backend.services.ProgressService;
import com.example.Backend.repository.UserRepository;
import com.example.Backend.model.SUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private ProgressService progressService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/home/{userId}")
    public ResponseEntity<Map<String, Object>> getHomeDashboard(@PathVariable String userId) {
        
        SUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User with ID " + userId + " not found"
                ));
        
        String realUsername = user.getUsername(); 
        
        Map<String, Object> data = progressService.getDashboardData(userId, realUsername);
        
        return ResponseEntity.ok(data);
    }
}