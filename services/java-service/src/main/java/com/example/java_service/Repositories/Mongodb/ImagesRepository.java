package com.example.java_service.Repositories;

import com.example.java_service.Models.Images;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ImagesRepository extends JpaRepository<Images, String>
{

}
