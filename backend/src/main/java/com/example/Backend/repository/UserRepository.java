package com.example.Backend.repository;

import com.example.Backend.model.SUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface UserRepository extends MongoRepository<SUser, String> {
    Optional<SUser> findByUsername(String username);
    Optional<SUser> findByEmail(String email);
}