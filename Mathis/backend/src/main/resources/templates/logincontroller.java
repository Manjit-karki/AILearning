/*package com.example.Backend.controller;

import com.example.Backend.DTO.LoginRequest;
import com.example.Backend.model.User;
import com.example.Backend.repository.userRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class logincontroller {
    @Autowired
    private final userRepository userRepo;
    @Autowired
    private AuthenticationManager authenticationManager;


    public logincontroller(userRepository userRepo) {
        this.userRepo = userRepo;
    }

    @PostMapping("/login")
    public ResponseEntity<?> signUser(@RequestBody User user) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            user.getPassword()
                    )
            );

            userRepo.findByUsername(user.getUsername())
                    .orElseThrow(() -> new RuntimeException("User data discrepancy encountered."));

            user.setPassword(user.getPassword());
            return ResponseEntity.ok().header("location","/home").build();

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
    }

}
*/