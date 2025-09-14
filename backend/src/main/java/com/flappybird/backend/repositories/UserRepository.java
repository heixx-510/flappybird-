package com.flappybird.backend.repositories;

import com.flappybird.backend.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);

    @Query(value = "{}", sort = "{ 'highscore' : -1 }")
    List<User> findTop10ByOrderByHighscoreDesc();
}
