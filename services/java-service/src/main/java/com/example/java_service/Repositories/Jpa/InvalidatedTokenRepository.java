package com.example.java_service.Repositories;

import com.example.java_service.Models.Invalidated_Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InvalidatedTokenRepository extends JpaRepository<Invalidated_Token, UUID> {
}