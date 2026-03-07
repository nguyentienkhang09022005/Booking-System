package com.example.java_service.Repositories.Mongodb;

import com.example.java_service.Dto.Response.AccommodationStatsResponse;
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
    Boolean existsByIdAccommodationAndIdUser(String idAccommodation, String idUser);

    // Lấy 5 đánh giá sớm nhất
    List<Evaluates> findTop3ByIdAccommodationOrderByCreatedAtAsc(String idAccommodation);

    Page<Evaluates> findByIdAccommodation(String idAccommodation, Pageable pageable);

    // Đếm số lượng đánh giá của Accommodation
    long countByIdAccommodation(String idAccommodation);

    // Tính trung bình số sao của Accommodation
    @Aggregation(pipeline = {
            "{ '$match': { 'idAccommodation': ?0 } }",
            "{ '$group': { '_id': null, 'avgStars': { '$avg': '$stars' } } }"
    })
    Double getAverageRatingByIdAccommodation(String idAccommodation);

    @Aggregation(pipeline = {
            "{ $match: { idAccommodation: { $in: ?0 } } }",
            "{ $group: { _id: '$idAccommodation', allEvaluates: { $sum: 1 }, averageStar: { $avg: '$stars' } } }",
            "{ $project: { idAccommodation: '$_id', allEvaluates: 1, averageStar: 1, _id: 0 } }"
    })
    List<AccommodationStatsResponse> findAccommodationStatsByAccommodationIds(List<String> accommodationIds);

    void deleteByIdUser(String idUser);

    void deleteByIdAccommodation(String idAccommodation);
}
