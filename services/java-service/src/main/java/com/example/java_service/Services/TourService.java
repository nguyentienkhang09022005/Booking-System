package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedStatusTour;
import com.example.java_service.Dto.Request.TourApproveRequest;
import com.example.java_service.Dto.Request.TourCreationRequest;
import com.example.java_service.Dto.Request.TourUpdateRequest;
import com.example.java_service.Dto.Response.OwnerTourResponse;
import com.example.java_service.Dto.Response.TourOverviewResponse;
import com.example.java_service.Dto.Response.TourResponse;
import com.example.java_service.Dto.Response.TourScheduleResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.TourMapper;
import com.example.java_service.Mappers.TourScheduleMapper;
import com.example.java_service.Mappers.UserMapper;
import com.example.java_service.Models.Images;
import com.example.java_service.Models.Tour;
import com.example.java_service.Models.TourSchedule;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.TourRepository;
import com.example.java_service.Repositories.Jpa.TourScheduleRepository;
import com.example.java_service.Repositories.Jpa.UserRepository;
import com.example.java_service.Repositories.Mongodb.ImagesRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    TourScheduleRepository tourScheduleRepository;
    UserRepository userRepository;
    ImagesRepository imagesRepository;
    TourMapper tourMapper;
    TourScheduleMapper tourScheduleMapper;
    UserMapper userMapper;

    private TourResponse mapToTourResponse(Tour tour)
    {
        TourResponse response = tourMapper.toTourResponse(tour);
        if (tour.getUsers() != null) {
            response.setOwnerTourResponse(userMapper.toOwnerTourResponse(tour.getUsers()));
        }

        List<String> imageUrls = imagesRepository.findByRefId(tour.getIdTour().toString())
                .stream()
                .map(Images::getUrl)
                .collect(Collectors.toList());
        response.setImageTourUrl(imageUrls);

        if (tour.getTourSchedules() != null && !tour.getTourSchedules().isEmpty()) {
            List<TourScheduleResponse> scheduleResponses = tour.getTourSchedules().stream()
                    .map(tourScheduleMapper::toTourScheduleResponse)
                    .collect(Collectors.toList());
            response.setTourScheduleResponses(scheduleResponses);
        }

        return response;
    }

    private TourOverviewResponse mapToTourOverviewResponse(Tour tour) {
        String thumbnail = null;
        List<Images> images = imagesRepository.findByRefId(tour.getIdTour().toString());
        if (!images.isEmpty()) {
            thumbnail = images.get(0).getUrl();
        }

        OwnerTourResponse owner = null;
        if (tour.getUsers() != null) {
            owner = userMapper.toOwnerTourResponse(tour.getUsers());
        }

        return TourOverviewResponse.builder()
                .idTour(tour.getIdTour())
                .tourName(tour.getTourName())
                .durationMinutes(tour.getDurationMinutes())
                .adultPrice(tour.getAdultPrice())
                .childPrice(tour.getChildPrice())
                .status(tour.getStatus())
                .approved(tour.getApproved())
                .createdAt(tour.getCreatedAt())
                .thumbnailImage(thumbnail)
                .ownerTourResponse(owner)
                .build();
    }

    public TourResponse getTourDetail(UUID idTour) {
        Tour tour = tourRepository.findById(idTour).orElseThrow(
                () -> new ApiException(ErrorCode.TOUR_NOT_FOUND)
        );
        return mapToTourResponse(tour);
    }

    public List<TourOverviewResponse> getAllTours() {
        return tourRepository.findAllToursWithSchedules().stream()
                .map(this::mapToTourOverviewResponse)
                .collect(Collectors.toList());
    }

    public List<TourOverviewResponse> getListTourApproved() {
        return tourRepository.findByApprovedTrue().stream()
                .map(this::mapToTourOverviewResponse)
                .collect(Collectors.toList());
    }

    public List<TourOverviewResponse> getListTourNotApproved() {
        return tourRepository.findByApprovedFalse().stream()
                .map(this::mapToTourOverviewResponse)
                .collect(Collectors.toList());
    }

    public List<TourOverviewResponse> getListTourCreatedByUser(UUID idUser)
    {
        return tourRepository.findByUsers_IdUser(idUser).stream()
                .map(this::mapToTourOverviewResponse)
                .collect(Collectors.toList());
    }

    public List<TourOverviewResponse> searchTours(String keyword)
    {
        List<Tour> tours = tourRepository.findByApprovedTrue();
        String lowerKeyword = keyword.toLowerCase();

        return tours.stream()
                .filter(tour -> tour.getTourName().toLowerCase().contains(lowerKeyword))
                .map(this::mapToTourOverviewResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ResponseEntity<String> saveTour(TourCreationRequest request)
    {
        Users user = userRepository.findById(request.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        if (request.getDurationMinutes() != null && request.getDurationMinutes() <= 0) {
            throw new ApiException(ErrorCode.INVALID_DURATION_DAYS);
        }
        if (request.getAdultPrice() != null && request.getAdultPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorCode.INVALID_ADULT_PRICE);
        }

        if (request.getChildPrice() != null && request.getChildPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorCode.INVALID_CHILD_PRICE);
        }

        Tour tour = tourMapper.toTour(request);
        tour.setUsers(user);
        tour.setApproved(false);
        tour.setStatus(PredefinedStatusTour.STATUS_TOUR_AVAILABLE);

        Tour savedTour = tourRepository.save(tour);

        // Lưu ảnh
        if (request.getImageTourUrl() != null && !request.getImageTourUrl().isEmpty()) {
            List<Images> images = request.getImageTourUrl().stream().map(url -> {
                Images image = new Images();
                image.setUrl(url);
                image.setRefId(savedTour.getIdTour().toString());
                return image;
            }).toList();
            imagesRepository.saveAll(images);
        }

        // Lưu lịch trình (nếu có)
        if (request.getTourScheduleRequests() != null && !request.getTourScheduleRequests().isEmpty()) {
            List<TourSchedule> schedules = request.getTourScheduleRequests().stream().map(scheduleReq -> {
                TourSchedule schedule = tourScheduleMapper.toTourSchedule(scheduleReq);
                schedule.setTour(savedTour);
                return schedule;
            }).toList();
            tourScheduleRepository.saveAll(schedules);
        }

        return ResponseEntity.ok("Tạo tour thành công!");
    }

    @Transactional
    public ResponseEntity<String> updateTour(UUID idTour, TourUpdateRequest request)
    {
        Tour tour = tourRepository.findById(idTour).orElseThrow(
                () -> new ApiException(ErrorCode.TOUR_NOT_FOUND)
        );

        if (request.getStatus() != null) {
            boolean isValidStatus = request.getStatus().equals(PredefinedStatusTour.STATUS_TOUR_AVAILABLE) ||
                    request.getStatus().equals(PredefinedStatusTour.STATUS_TOUR_SOLD_OUT);
            if (!isValidStatus) {
                throw new ApiException(ErrorCode.INVALID_STATUS_TOUR);
            }
        }

        if (request.getDurationMinutes() != null && request.getDurationMinutes() <= 0) {
            throw new ApiException(ErrorCode.INVALID_DURATION_DAYS);
        }
        if (request.getAdultPrice() != null && request.getAdultPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorCode.INVALID_ADULT_PRICE);
        }

        if (request.getChildPrice() != null && request.getChildPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorCode.INVALID_CHILD_PRICE);
        }

        tourMapper.updateTourFromRequest(request, tour);
        Tour updatedTour = tourRepository.save(tour);

        // Cập nhật ảnh (xóa cũ thêm mới)
        if (request.getImageTourUrl() != null && !request.getImageTourUrl().isEmpty()) {
            imagesRepository.deleteByRefId(updatedTour.getIdTour().toString());

            List<Images> images = request.getImageTourUrl().stream().map(url -> {
                Images image = new Images();
                image.setUrl(url);
                image.setRefId(updatedTour.getIdTour().toString());
                return image;
            }).toList();
            imagesRepository.saveAll(images);
        }

        // Cập nhật lịch trình (xóa cũ thêm mới)
        if (request.getTourScheduleRequests() != null && !request.getTourScheduleRequests().isEmpty()) {
            tourScheduleRepository.deleteByTour_IdTour(idTour);

            List<TourSchedule> schedules = request.getTourScheduleRequests().stream().map(scheduleReq -> {
                TourSchedule schedule = tourScheduleMapper.toTourSchedule(scheduleReq);
                schedule.setTour(updatedTour);
                return schedule;
            }).toList();
            tourScheduleRepository.saveAll(schedules);
        }

        return ResponseEntity.ok("Cập nhật thông tin tour thành công!");
    }

    @Transactional
    public ResponseEntity<String> deleteTour(UUID idTour)
    {
        Tour tour = tourRepository.findById(idTour).orElseThrow(
                () -> new ApiException(ErrorCode.TOUR_NOT_FOUND)
        );

        imagesRepository.deleteByRefId(idTour.toString());
        tourRepository.delete(tour); // Sẽ cascade xóa luôn bảng TourSchedule (nếu dùng OneToMany cascade)

        return ResponseEntity.ok("Xóa tour thành công!");
    }

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