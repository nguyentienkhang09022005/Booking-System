package com.example.java_service.Repositories.Mongodb;

import com.example.java_service.Models.Favorite;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRepository extends MongoRepository<Favorite, String>
{
    List<Favorite> findByIdUser(String idUser);

    Boolean existsByIdUserAndIdAccommodation(String idUser, String idAccommodation);

    void deleteByIdUser(String idUser);

    void deleteByIdAccommodation(String idAccommodation);

    void deleteByIdUserAndIdAccommodation(String idUser, String idAccommodation);
}
