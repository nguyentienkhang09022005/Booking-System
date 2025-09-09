package com.example.java_service.Repositories.Mongodb;

import com.example.java_service.Models.Images;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImagesRepository extends MongoRepository<Images, String>
{

}
