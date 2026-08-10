/*
package com.example.Backend.controller;
import com.example.Backend.DTO.LoginRequest;
import com.example.Backend.repository.userRepository;
import com.example.Backend.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


@RestController
public class usercontroller {
    @Autowired
    private final userRepository userRepo;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public usercontroller(userRepository userRepo) {
        this.userRepo = userRepo;
    }


    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody User user) {
        String hashedPassword=passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);
        User savedUser=userRepo.save(user);

        return ResponseEntity.ok(savedUser);
    }


    @PostMapping("/login")
    public ResponseEntity<?> signUser(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            User user = userRepo.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("User data discrepancy encountered."));

            user.setPassword(loginRequest.getPassword());

            return ResponseEntity.ok(user);

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
    }

    @GetMapping("/home")
    public String welcome() {
        return "welcome";
    }
}
public DocumentIngestionService(VectorStore vectorStore) {this.vectorStore = vectorStore;}
*/
