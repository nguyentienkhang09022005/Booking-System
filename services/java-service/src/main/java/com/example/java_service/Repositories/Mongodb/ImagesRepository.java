package com.example.java_service.Repositories.Mongodb;

import com.example.java_service.Models.Images;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImagesRepository extends MongoRepository<Images, String>
{
    void deleteByRefId(String refId);

    List<Images> findByRefIdOrderByUploadedAtAsc(String idHotel);
}
