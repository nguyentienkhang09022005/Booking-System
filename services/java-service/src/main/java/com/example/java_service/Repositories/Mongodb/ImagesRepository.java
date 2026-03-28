package com.example.java_service.Repositories.Mongodb;

import com.example.java_service.Models.Images;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImagesRepository extends MongoRepository<Images, String>
{
    void deleteByRefId(String refId);

    void deleteAllByRefId(String refIds);

    List<Images> findByRefIdOrderByUploadedAtAsc(String idAccommodation);

    List<Images> findByRefIdIn(List<String> refIds);

    List<Images> findByRefId(String refId);
}
