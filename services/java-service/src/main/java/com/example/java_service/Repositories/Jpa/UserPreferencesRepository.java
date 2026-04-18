package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {
    Optional<UserPreferences> findByUsers_IdUser(UUID idUser);
    Boolean existsByUsers_IdUser(UUID idUser);
    void deleteByUsers_IdUser(UUID idUser);
}