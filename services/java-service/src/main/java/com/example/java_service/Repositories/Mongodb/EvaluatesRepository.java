package com.example.java_service.Repositories.Mongodb;

import com.example.java_service.Dto.Response.EvaluateStatsResponse;
import com.example.java_service.Models.Evaluates;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluatesRepository extends MongoRepository<Evaluates, String> {

    Boolean existsByRefIdAndRefTypeAndIdUser(String refId, String refType, String idUser);

    // Lấy top 3 đánh giá sớm nhất theo refId và refType
    List<Evaluates> findTop3ByRefIdAndRefTypeOrderByCreatedAtAsc(String refId, String refType);

    Page<Evaluates> findByRefIdAndRefType(String refId, String refType, Pageable pageable);

    // Đếm số lượng đánh giá
    long countByRefIdAndRefType(String refId, String refType);

    // Tính trung bình số sao
    @Aggregation(pipeline = {
            "{ '$match': { 'refId': ?0, 'refType': ?1 } }",
            "{ '$group': { '_id': null, 'avgStars': { '$avg': '$stars' } } }"
    })
    Double getAverageRatingByRefIdAndRefType(String refId, String refType);

    // Lấy thống kê cho danh sách refIds
    @Aggregation(pipeline = {
            "{ $match: { refId: { $in: ?0 }, refType: ?1 } }",
            "{ $group: { _id: '$refId', allEvaluates: { $sum: 1 }, averageStar: { $avg: '$stars' } } }",
            "{ $project: { refId: '$_id', allEvaluates: 1, averageStar: 1, _id: 0 } }"
    })
    List<EvaluateStatsResponse> findStatsByRefIdsAndRefType(List<String> refIds, String refType);

    void deleteByIdUser(String idUser);

    void deleteByRefIdAndRefType(String refId, String refType);
}