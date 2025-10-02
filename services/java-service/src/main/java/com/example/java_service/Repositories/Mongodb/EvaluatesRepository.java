package com.example.java_service.Repositories.Mongodb;

import com.example.java_service.Dto.Response.HotelStatsResponse;
import com.example.java_service.Models.Evaluates;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluatesRepository extends MongoRepository<Evaluates, String>
{
    Boolean existsByIdHotelAndIdUser(String idHotel, String idUser);

    // Lấy 5 đánh giá sớm nhất
    List<Evaluates> findTop3ByIdHotelOrderByCreatedAtAsc(String idHotel);

    Page<Evaluates> findByIdHotel(String idHotel, Pageable pageable);

    // Đếm số lượng đánh giá của hotel
    long countByIdHotel(String idHotel);

    // Tính trung bình số sao của hotel
    @Aggregation(pipeline = {
            "{ '$match': { 'idHotel': ?0 } }",
            "{ '$group': { '_id': null, 'avgStars': { '$avg': '$stars' } } }"
    })
    Double getAverageRatingByIdHotel(String idHotel);

    @Aggregation(pipeline = {
            "{ $match: { idHotel: { $in: ?0 } } }",
            "{ $group: { _id: '$idHotel', allEvaluates: { $sum: 1 }, averageStar: { $avg: '$stars' } } }",
            "{ $project: { idHotel: '$_id', allEvaluates: 1, averageStar: 1, _id: 0 } }"
    })
    List<HotelStatsResponse> findHotelStatsByHotelIds(List<String> hotelIds);

    void deleteByIdUserAndIdHotel(String idUser, String idHotel);

    void deleteByIdUser(String idUser);
}
