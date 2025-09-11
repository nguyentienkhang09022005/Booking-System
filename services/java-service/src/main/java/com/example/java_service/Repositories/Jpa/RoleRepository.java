package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Roles, String>
{
}
