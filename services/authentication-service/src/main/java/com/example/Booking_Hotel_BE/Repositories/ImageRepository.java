package com.example.Booking_Hotel_BE.Repositories;

import com.example.Booking_Hotel_BE.Models.Images;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends MongoRepository<Images, String> {
    List<Images> findByRefId(String refId);
}
