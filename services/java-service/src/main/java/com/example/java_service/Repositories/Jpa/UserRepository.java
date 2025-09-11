package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<Users, UUID>
{
    Optional<Users> findByEmailOrPhone(String email, String phone);
}
