package com.example.java_service.Repositories.Mongodb;

import com.example.java_service.Models.Favorite;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRepository extends MongoRepository<Favorite, String>
{
    List<Favorite> findByIdUser(String idUser);

    Boolean existsByIdUserAndIdHotel(String idUser, String idHotel);

    void deleteByIdUser(String idUser);

    void deleteByIdHotel(String idHotel);
}
