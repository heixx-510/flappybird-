package com.flappybird.backend.controllers;

import com.flappybird.backend.dto.LoginRequest;
import com.flappybird.backend.dto.ScoreUpdateRequest;
import com.flappybird.backend.models.User;
import com.flappybird.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow all origins
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) {
        try {
            Optional<User> userOptional = userRepository.findByUsername(loginRequest.getUsername());

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                if (user.getPassword().equals(loginRequest.getPassword())) {
                    return ResponseEntity.ok("Login successful");
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Wrong password");
                }
            } else {
                // Create new user
                User newUser = new User(loginRequest.getUsername(), loginRequest.getPassword());
                userRepository.save(newUser);
                return ResponseEntity.ok("New user created");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error: " + e.getMessage());
        }
    }

    @PostMapping("/updateScore")
    public ResponseEntity<String> updateScore(@RequestBody ScoreUpdateRequest scoreUpdate) {
        try {
            Optional<User> userOptional = userRepository.findByUsername(scoreUpdate.getUsername());

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                if (scoreUpdate.getScore() > user.getHighscore()) {
                    user.setHighscore(scoreUpdate.getScore());
                    userRepository.save(user);
                    return ResponseEntity.ok("Score updated");
                }
                return ResponseEntity.ok("Score not higher than current highscore");
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error: " + e.getMessage());
        }
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<User>> getLeaderboard() {
        try {
            List<User> topUsers = userRepository.findTop10ByOrderByHighscoreDesc();
            return ResponseEntity.ok(topUsers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}