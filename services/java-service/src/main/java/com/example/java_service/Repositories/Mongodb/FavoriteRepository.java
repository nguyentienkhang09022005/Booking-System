package com.example.java_service.Repositories.Mongodb;

import com.example.java_service.Models.Favorite;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRepository extends MongoRepository<Favorite, String> {

    List<Favorite> findByIdUser(String idUser);

    Boolean existsByIdUserAndRefIdAndRefType(String idUser, String refId, String refType);

    void deleteByIdUser(String idUser);

    void deleteByRefIdAndRefType(String refId, String refType);

    void deleteByIdUserAndRefIdAndRefType(String idUser, String refId, String refType);

    List<Favorite> findByIdUserAndRefType(String idUser, String refType);
}