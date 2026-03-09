package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedStatusTour;
import com.example.java_service.Dto.Request.TourApproveRequest;
import com.example.java_service.Dto.Request.TourCreationRequest;
import com.example.java_service.Dto.Request.TourUpdateRequest;
import com.example.java_service.Dto.Response.TourResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.TourMapper;
import com.example.java_service.Mappers.UserMapper;
import com.example.java_service.Models.Tour;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.TourRepository;
import com.example.java_service.Repositories.Jpa.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TourService
{
    TourRepository tourRepository;
    UserRepository userRepository;
    TourMapper tourMapper;
    UserMapper userMapper;

    private TourResponse mapToTourResponse(Tour tour)
    {
        TourResponse response = tourMapper.toTourResponse(tour);
        if (tour.getUsers() != null) {
            response.setOwnerTourResponse(userMapper.toOwnerTourResponse(tour.getUsers()));
        }
        return response;
    }

    // Lấy danh sách tất cả tour
    public List<TourResponse> getAllTours()
    {
        return tourRepository.findAll().stream()
                .map(this::mapToTourResponse)
                .collect(Collectors.toList());
    }

    // Lấy danh sách tour đã duyệt
    public List<TourResponse> getListTourApproved()
    {
        return tourRepository.findByApprovedTrue().stream()
                .map(this::mapToTourResponse)
                .collect(Collectors.toList());
    }

    // Lấy danh sách tour chưa duyệt
    public List<TourResponse> getListTourNotApproved()
    {
        return tourRepository.findByApprovedFalse().stream()
                .map(this::mapToTourResponse)
                .collect(Collectors.toList());
    }

    // Lấy danh sách tour của user đã tạo
    public List<TourResponse> getListTourCreatedByUser(UUID idUser)
    {
        return tourRepository.findByUsers_IdUser(idUser).stream()
                .map(this::mapToTourResponse)
                .collect(Collectors.toList());
    }

    // Thêm tour mới
    @Transactional
    public ResponseEntity<String> saveTour(TourCreationRequest request)
    {
        Users user = userRepository.findById(request.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        // Validate durationDays
        if (request.getDurationDays() != null && request.getDurationDays() <= 0)
        {
            throw new ApiException(ErrorCode.INVALID_DURATION_DAYS);
        }

        Tour tour = tourMapper.toTour(request);
        tour.setUsers(user);
        tour.setApproved(false); // Mặc định chưa phê duyệt
        tour.setStatus(PredefinedStatusTour.STATUS_TOUR_AVAILABLE); // Trạng thái mặc định khi tạo mới

        tourRepository.save(tour);
        return ResponseEntity.ok("Tạo tour thành công!");
    }

    // Sửa thông tin tour
    @Transactional
    public ResponseEntity<String> updateTour(UUID idTour, TourUpdateRequest request)
    {
        Tour tour = tourRepository.findById(idTour).orElseThrow(
                () -> new ApiException(ErrorCode.TOUR_NOT_FOUND)
        );

        // Validate status
        if (request.getStatus() != null) {
            boolean isValidStatus = request.getStatus().equals(PredefinedStatusTour.STATUS_TOUR_AVAILABLE) ||
                    request.getStatus().equals(PredefinedStatusTour.STATUS_TOUR_SOLD_OUT);
            if (!isValidStatus) {
                throw new ApiException(ErrorCode.INVALID_STATUS_TOUR);
            }
        }

        // Validate durationDays
        if (request.getDurationDays() != null && request.getDurationDays() <= 0)
        {
            throw new ApiException(ErrorCode.INVALID_DURATION_DAYS);
        }

        tourMapper.updateTourFromRequest(request, tour);
        tourRepository.save(tour);

        return ResponseEntity.ok("Cập nhật thông tin tour thành công!");
    }

    // Xóa tour
    @Transactional
    public ResponseEntity<String> deleteTour(UUID idTour)
    {
        Tour tour = tourRepository.findById(idTour).orElseThrow(
                () -> new ApiException(ErrorCode.TOUR_NOT_FOUND)
        );

        tourRepository.delete(tour);
        return ResponseEntity.ok("Xóa tour thành công!");
    }

    // Phê duyệt tour
    @Transactional
    public ResponseEntity<String> approveTour(UUID idTour, TourApproveRequest request)
    {
        Tour tour = tourRepository.findById(idTour).orElseThrow(
                () -> new ApiException(ErrorCode.TOUR_NOT_FOUND)
        );

        tour.setApproved(request.getApproved());
        tourRepository.save(tour);

        String message = request.getApproved() ? "Phê duyệt tour thành công!" : "Đã hủy phê duyệt tour!";
        return ResponseEntity.ok(message);
    }
}