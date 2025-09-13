package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.TypeUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TypeUserRepository extends JpaRepository<TypeUser, String>
{
}
