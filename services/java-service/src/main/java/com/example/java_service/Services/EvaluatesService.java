package com.example.java_service.Services;

import com.example.java_service.Dto.Request.EvaluateCreationRequest;
import com.example.java_service.Dto.Request.EvaluateUpdateRequest;
import com.example.java_service.Dto.Response.EvaluateResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.EvaluateMapper;
import com.example.java_service.Models.Evaluates;
import com.example.java_service.Repositories.Jpa.HotelRepository;
import com.example.java_service.Repositories.Jpa.UserRepository;
import com.example.java_service.Repositories.Mongodb.EvaluatesRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EvaluatesService
{
    EvaluatesRepository evaluatesRepository;
    HotelRepository hotelRepository;
    UserRepository userRepository;
    EvaluateMapper evaluateMapper;

    // Hàm lấy danh sách đánh giá khách sạn theo phân trang 5
    public Page<EvaluateResponse> getListEvaluateByPaging(Pageable pageable, UUID idHotel)
    {
        if (!hotelRepository.existsById(idHotel)) {
            throw new ApiException(ErrorCode.HOTEL_NOT_FOUND);
        }

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                3,
                Sort.by("createdAt").descending()
        );
        Page<Evaluates> evaluatesPage = evaluatesRepository.findByIdHotel(idHotel.toString(), sortedPageable);

        // Map kết quả của trang hiện tại sang EvaluateResponse
        List<EvaluateResponse> responses = evaluatesPage.getContent().stream()
                .map(evaluateMapper::toEvaluateResponse)
                .toList();

        return new PageImpl<>(responses, evaluatesPage.getPageable(), evaluatesPage.getTotalElements());
    }

    // Hàm tạo đánh giá
    public EvaluateResponse saveEvaluate(EvaluateCreationRequest request)
    {
        hotelRepository.findById(UUID.fromString(request.getIdHotel())).orElseThrow(
                () -> new ApiException(ErrorCode.HOTEL_NOT_FOUND)
        );
        userRepository.findById(UUID.fromString(request.getIdUser())).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );
        Boolean checkEvaluate = evaluatesRepository.existsByIdHotelAndIdUser(request.getIdHotel(), request.getIdUser());
        if (checkEvaluate)
            throw new ApiException(ErrorCode.USER_EVALUATED_HOTEL);

        Evaluates evaluates = evaluateMapper.toEvaluate(request);
        evaluates.setCreatedAt(LocalDateTime.now());
        evaluatesRepository.save(evaluates);

        evaluateMapper.toEvaluateResponse(evaluates);
        return evaluateMapper.toEvaluateResponse(evaluates);
    }

    // Hàm sửa đánh giá
    public EvaluateResponse updateEvaluate(EvaluateUpdateRequest request, String id)
    {
        Evaluates evaluate = evaluatesRepository.findById(id).orElseThrow(
                () -> new ApiException(ErrorCode.EVALUATE_NOT_FOUND)
        );
        // Kiểm tra thời gian
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = evaluate.getCreatedAt().plusMinutes(30);

        if (now.isAfter(deadline)) {
            throw new ApiException(ErrorCode.EVALUATE_UPDATE_TIME_EXPIRED);
        }

        evaluateMapper.updateEvaluateFromRequest(request, evaluate);
        evaluatesRepository.save(evaluate);
        return evaluateMapper.toEvaluateResponse(evaluate);
    }

    // Hàm xóa đánh giá
    public ResponseEntity<String> deleteEvaluate(String id)
    {
        Evaluates evaluate = evaluatesRepository.findById(id).orElseThrow(
                () -> new ApiException(ErrorCode.EVALUATE_NOT_FOUND)
        );
        evaluatesRepository.delete(evaluate);
        return ResponseEntity.ok("Xóa đánh giá thành công!");
    }

    // Hàm lấy tất cả số đánh giá của khách sạn
    public Integer getAllEvaluateOfHotel(UUID idHotel)
    {
        if (!hotelRepository.existsById(idHotel)) {
            throw new ApiException(ErrorCode.HOTEL_NOT_FOUND);
        }
        return (int) evaluatesRepository.countByIdHotel(idHotel.toString());
    }

    // Hàm lấy trung bình số sao của khách sạn
    public Double getAverageStarOfHotel(UUID idHotel)
    {
        if (!hotelRepository.existsById(idHotel)) {
            throw new ApiException(ErrorCode.HOTEL_NOT_FOUND);
        }

        Double averageOfHotel = evaluatesRepository.getAverageRatingByIdHotel(idHotel.toString());
        return averageOfHotel != null ? averageOfHotel : 0.0;
    }
}
