package com.flappybird.backend.dto;

public class ScoreUpdateRequest {
    private String username;
    private int score;

    // Constructors
    public ScoreUpdateRequest() {}

    public ScoreUpdateRequest(String username, int score) {
        this.username = username;
        this.score = score;
    }

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
}
