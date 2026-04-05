package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedRefFavoriteAndEvaluate;
import com.example.java_service.Dto.Request.AccommodationCreationRequest;
import com.example.java_service.Dto.Request.AccommodationUpdateRequest;
import com.example.java_service.Dto.Response.*;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.EvaluateMapper;
import com.example.java_service.Mappers.AccommodationMapper;
import com.example.java_service.Mappers.UserMapper;
import com.example.java_service.Models.*;
import com.example.java_service.Repositories.Jpa.*;
import com.example.java_service.Repositories.Mongodb.EvaluatesRepository;
import com.example.java_service.Repositories.Mongodb.FavoriteRepository;
import com.example.java_service.Repositories.Mongodb.ImagesRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccommodationService
{
    UserRepository userRepository;
    AccommodationRepository accommodationRepository;
    FavoriteRepository favoriteRepository;
    ImagesRepository imagesRepository;
    EvaluatesRepository evaluatesRepository;
    FacilityCategoryRepository facilityCategoryRepository;
    GeneralRegulationRepository generalRegulationRepository;
    EvaluatesService evaluatesService;
    RoomService roomService;
    AccommodationMapper accommodationMapper;
    UserMapper userMapper;
    EvaluateMapper evaluateMapper;

    // Hàm lấy danh sách nơi cư trú đã được phê duyệt
    public List<GetListAccommodationResponse> getListAccommodationApproved(UUID idUser) {
        List<Accommodation> accommodationList = accommodationRepository.findAllByApprovedTrue();

        if (accommodationList.isEmpty()) {
            return List.of();
        }

        final Set<String> favoriteAccommodationIds;
        if (idUser != null) {
            Users user = userRepository.findById(idUser).orElseThrow(
                    () -> new ApiException(ErrorCode.USER_NOT_FOUND));
            favoriteAccommodationIds = favoriteRepository.findByIdUser(user.getIdUser().toString())
                    .stream()
                    .filter(fav -> PredefinedRefFavoriteAndEvaluate.REF_ACCOMMODATION.equalsIgnoreCase(fav.getRefType())) // Chỉ lấy những Favorite là accommodation
                    .map(Favorite::getRefId) // Đổi từ getIdAccommodation sang getRefId
                    .collect(Collectors.toSet());
        } else {
            favoriteAccommodationIds = Set.of();
        }

        // Lấy ID của tất cả nơi cư trú
        List<String> accommodationIds = accommodationList.stream()
                .map(accommodation -> accommodation.getIdAccommodation().toString())
                .toList();

        // Lấy tất cả ảnh
        List<Images> images = imagesRepository.findByRefIdIn(accommodationIds);
        Map<String, List<String>> accommodationImagesMap = images.stream()
                .collect(Collectors.groupingBy(
                        Images::getRefId,
                        Collectors.mapping(Images::getUrl, Collectors.toList())
                ));

        // Lấy thống kê truyền refType là "accommodation"
        Map<String, EvaluateStatsResponse> statsMap = evaluatesRepository.findStatsByRefIdsAndRefType(accommodationIds, PredefinedRefFavoriteAndEvaluate.REF_ACCOMMODATION)
                .stream()
                .collect(Collectors.toMap(EvaluateStatsResponse::getRefId, dto -> dto));

        return accommodationList.stream()
                .map(accommodation -> {
                    GetListAccommodationResponse response = accommodationMapper.toGetListAccommodationResponse(accommodation);
                    String accommodationIdStr = accommodation.getIdAccommodation().toString();

                    EvaluateStatsResponse stats = statsMap.get(accommodationIdStr);

                    response.setFavorite(favoriteAccommodationIds.contains(accommodationIdStr));
                    response.setImageAccommodationUrl(accommodationImagesMap.get(accommodationIdStr));

                    if (stats != null) {
                        response.setAllEvaluates(stats.getAllEvaluates().intValue());
                        response.setAverageStar(stats.getAverageStar());
                    } else {
                        response.setAllEvaluates(0);
                        response.setAverageStar(0.0);
                    }

                    return response;
                })
                .toList();
    }

    // Hàm lấy danh sách nơi cư trú chưa phê duyệt
    public List<GetListAccommodationResponse> getListAccommodationNotApproved() {
        List<Accommodation> accommodationList = accommodationRepository.findAllByApprovedFalse();

        if (accommodationList.isEmpty()) {
            return List.of();
        }

        // Lấy ID của tất cả nơi cư trú
        List<String> accommodationIds = accommodationList.stream()
                .map(accommodation -> accommodation.getIdAccommodation().toString())
                .toList();

        // Lấy tất cả ảnh
        List<Images> images = imagesRepository.findByRefIdIn(accommodationIds);
        Map<String, List<String>> accommodationImagesMap = images.stream()
                .collect(Collectors.groupingBy(
                        Images::getRefId,
                        Collectors.mapping(Images::getUrl, Collectors.toList())
                ));

        // Lấy thống kê
        Map<String, EvaluateStatsResponse> statsMap = evaluatesRepository.findStatsByRefIdsAndRefType(accommodationIds, PredefinedRefFavoriteAndEvaluate.REF_ACCOMMODATION)
                .stream()
                .collect(Collectors.toMap(EvaluateStatsResponse::getRefId, dto -> dto));

        return accommodationList.stream()
                .map(accommodation -> {
                    GetListAccommodationResponse response = accommodationMapper.toGetListAccommodationResponse(accommodation);
                    String accommodationIdStr = accommodation.getIdAccommodation().toString();

                    EvaluateStatsResponse stats = statsMap.get(accommodationIdStr);

                    response.setImageAccommodationUrl(accommodationImagesMap.get(accommodationIdStr));

                    if (stats != null) {
                        response.setAllEvaluates(stats.getAllEvaluates().intValue());
                        response.setAverageStar(stats.getAverageStar());
                    } else {
                        response.setAllEvaluates(0);
                        response.setAverageStar(0.0);
                    }

                    return response;
                })
                .toList();
    }

    public List<GetListAccommodationResponse> searchAccommodations(String keyword)
    {
        List<Accommodation> accommodations = accommodationRepository.findAllByApprovedTrue();
        String lowerKeyword = keyword.toLowerCase();

        return accommodations.stream()
                .filter(acc -> acc.getAccommodationName().toLowerCase().contains(lowerKeyword) ||
                        acc.getCity().toLowerCase().contains(lowerKeyword) ||
                        acc.getProvince().toLowerCase().contains(lowerKeyword) ||
                        (acc.getUsers().getAddress() != null && acc.getUsers().getAddress().toLowerCase().contains(lowerKeyword)))
                .map(acc -> {
                    GetListAccommodationResponse response = accommodationMapper.toGetListAccommodationResponse(acc);
                    List<String> imageUrls = imagesRepository.findByRefId(acc.getIdAccommodation().toString())
                            .stream()
                            .map(Images::getUrl)
                            .toList();
                    response.setImageAccommodationUrl(imageUrls);
                    return response;
                })
                .toList();
    }

    // Hàm lấy thông tin chi tiết của nơi cư trú qua id (bổ sung lấy thêm evaluates top 5 gần nhất)
    public GetDetailAccommodationResponse getAccommodationDetail(UUID idAccommodation, UUID idUser)
    {
        Accommodation accommodation = accommodationRepository.findById(idAccommodation).orElseThrow(
                () -> new ApiException(ErrorCode.ACCOMMODATION_NOT_FOUND)
        );

        Boolean favoriteCheck;
        if (idUser != null) {
            userRepository.findById(idUser).orElseThrow(
                    () -> new ApiException(ErrorCode.USER_NOT_FOUND));
            favoriteCheck = favoriteRepository.existsByIdUserAndRefIdAndRefType(
                    idUser.toString(),
                    idAccommodation.toString(),
                    PredefinedRefFavoriteAndEvaluate.REF_ACCOMMODATION);
        } else {
            favoriteCheck = false;
        }

        List<String> imageUrls = imagesRepository.findByRefId(accommodation.getIdAccommodation().toString())
                .stream()
                .map(Images::getUrl)
                .toList();

        OwnerAccommodationResponse ownerAccommodationResponse = userMapper.toOwnerAccommodationResponse(accommodation.getUsers());

        // Lấy tất cả đánh giá của nơi cư trú (Cập nhật gọi hàm mới của EvaluatesService)
        Integer allEvaluate = evaluatesService.getAllEvaluate(idAccommodation.toString(), PredefinedRefFavoriteAndEvaluate.REF_ACCOMMODATION);
        // Lấy trung bình số sao của nơi cư trú (Cập nhật gọi hàm mới của EvaluatesService)
        Double stars = evaluatesService.getAverageStar(idAccommodation.toString(), PredefinedRefFavoriteAndEvaluate.REF_ACCOMMODATION);

        // Lấy danh sách đánh giá top 3
        List<Evaluates> top3Evaluates = evaluatesRepository.findTop3ByRefIdAndRefTypeOrderByCreatedAtAsc(idAccommodation.toString(), PredefinedRefFavoriteAndEvaluate.REF_ACCOMMODATION);
        List<EvaluateResponse> evaluateResponses = top3Evaluates.stream()
                .map(evaluateMapper::toEvaluateResponse)
                .toList();

        // Lấy categories + facilities
        List<GetDetailAccommodationResponse.FacilityCategoryResponse> facilityCategoryResponses =
                facilityCategoryRepository.findByAccommodation_IdAccommodation(idAccommodation)
                        .stream()
                        .map(cat -> GetDetailAccommodationResponse.FacilityCategoryResponse.builder()
                                .idCategory(cat.getIdCategory())
                                .categoryName(cat.getCategoryName())
                                .facilities(
                                        cat.getFacilities().stream()
                                                .map(fac -> GetDetailAccommodationResponse.FacilityResponse.builder()
                                                        .idFacility(fac.getIdFacility())
                                                        .facilityName(fac.getFacilityName())
                                                        .isFree(fac.getIsFree())
                                                        .build())
                                                .toList()
                                )
                                .build())
                        .toList();

        // Lấy general regulations
        List<GetDetailAccommodationResponse.GeneralRegulationResponse> regulationResponses =
                generalRegulationRepository.findByAccommodation_IdAccommodation(idAccommodation)
                        .stream()
                        .map(reg -> GetDetailAccommodationResponse.GeneralRegulationResponse.builder()
                                .idRegulation(reg.getIdRegulation())
                                .regulationName(reg.getRegulationName())
                                .regulationContent(reg.getRegulationContent())
                                .build())
                        .toList();

        // Lấy danh sách phòng thuộc nơi cư trú
        List<GetRoomResponse> listRoomOfAccommodation = roomService.getListRoom(idAccommodation);

        GetDetailAccommodationResponse accommodationResponse = accommodationMapper.toGetDetailAccommodationResponse(accommodation);
        accommodationResponse.setOwnerAccommodationResponse(ownerAccommodationResponse);
        accommodationResponse.setImageAccommodation(imageUrls);
        accommodationResponse.setFavorite(favoriteCheck);
        accommodationResponse.setEvaluates(evaluateResponses);
        accommodationResponse.setAllEvaluates(allEvaluate);
        accommodationResponse.setAverageStar(stars);
        accommodationResponse.setRoomResponses(listRoomOfAccommodation);
        accommodationResponse.setFacilityCategoryResponses(facilityCategoryResponses);
        accommodationResponse.setGeneralRegulationResponses(regulationResponses);
        return accommodationResponse;
    }

    public List<GetListAccommodationResponse> getListAccommodationCreatedByUser(UUID idUser) {
        List<Accommodation> accommodationList = accommodationRepository.findByUsers_IdUser(idUser);
        if (accommodationList.isEmpty()) {
            return List.of();
        }

        final Set<String> favoriteAccommodationIds;
        if (idUser != null) {
            Users user = userRepository.findById(idUser).orElseThrow(
                    () -> new ApiException(ErrorCode.USER_NOT_FOUND));
            favoriteAccommodationIds = favoriteRepository.findByIdUser(user.getIdUser().toString())
                    .stream()
                    .filter(fav -> PredefinedRefFavoriteAndEvaluate.REF_ACCOMMODATION.equalsIgnoreCase(fav.getRefType()))
                    .map(Favorite::getRefId)
                    .collect(Collectors.toSet());
        } else {
            favoriteAccommodationIds = Set.of();
        }

        // Lấy ID của tất cả nơi cư trú
        List<String> accommodationIds = accommodationList.stream()
                .map(accommodation -> accommodation.getIdAccommodation().toString())
                .toList();

        // Lấy tất cả ảnh
        List<Images> images = imagesRepository.findByRefIdIn(accommodationIds);
        Map<String, List<String>> accommodationImagesMap = images.stream()
                .collect(Collectors.groupingBy(
                        Images::getRefId,
                        Collectors.mapping(Images::getUrl, Collectors.toList())
                ));

        // Lấy thống kê
        Map<String, EvaluateStatsResponse> statsMap = evaluatesRepository.findStatsByRefIdsAndRefType(accommodationIds, PredefinedRefFavoriteAndEvaluate.REF_ACCOMMODATION)
                .stream()
                .collect(Collectors.toMap(EvaluateStatsResponse::getRefId, dto -> dto));

        return accommodationList.stream()
                .map(accommodation -> {
                    GetListAccommodationResponse response = accommodationMapper.toGetListAccommodationResponse(accommodation);
                    String accommodationIdStr = accommodation.getIdAccommodation().toString();

                    EvaluateStatsResponse stats = statsMap.get(accommodationIdStr);

                    response.setFavorite(favoriteAccommodationIds.contains(accommodationIdStr));
                    response.setImageAccommodationUrl(accommodationImagesMap.get(accommodationIdStr));

                    if (stats != null) {
                        response.setAllEvaluates(stats.getAllEvaluates().intValue());
                        response.setAverageStar(stats.getAverageStar());
                    } else {
                        response.setAllEvaluates(0);
                        response.setAverageStar(0.0);
                    }
                    return response;
                })
                .toList();
    }

    // Thêm nơi cư trú mới
    @Transactional
    public ResponseEntity<String> saveAccommodation(AccommodationCreationRequest request)
    {
        Users user = userRepository.findById(request.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        if (accommodationRepository.existsByUsers_IdUser(user.getIdUser())){
            throw new ApiException(ErrorCode.USER_CREATED_ACCOMMODATION);
        }

        Accommodation accommodation = accommodationMapper.toAccommodation(request);
        accommodation.setUsers(user);
        accommodation.setApproved(false);
        Accommodation savedAccommodation = accommodationRepository.save(accommodation);

        // Lưu ảnh từ URL đã upload
        if (request.getListImageAccommodation() != null && !request.getListImageAccommodation().isEmpty()) {
            List<Images> images = request.getListImageAccommodation().stream().map(url -> {
                Images image = new Images();
                image.setUrl(url);
                image.setRefId(savedAccommodation.getIdAccommodation().toString());
                return image;
            }).toList();
            imagesRepository.saveAll(images);
        }

        if (request.getFacilityCategories() != null && !request.getFacilityCategories().isEmpty())
        {
            List<FacilityCategory> categories = request.getFacilityCategories()
                    .stream()
                    .map(category -> {
                        FacilityCategory facilityCategory = FacilityCategory.builder()
                                .categoryName(category.getCategoryName())
                                .accommodation(savedAccommodation)
                                .build();

                        if (category.getFacilities() != null && !category.getFacilities().isEmpty()) {
                            List<Facility> facilities = category.getFacilities().stream()
                                    .map(facilityRequest -> Facility.builder()
                                            .facilityName(facilityRequest.getFacilityName())
                                            .isFree(facilityRequest.getIsFree())
                                            .category(facilityCategory)
                                            .build())
                                    .toList();
                            facilityCategory.setFacilities(facilities);
                        }
                        return facilityCategory;
                    }).toList();
            facilityCategoryRepository.saveAll(categories);
        }

        if (request.getGeneralRegulations() != null && !request.getGeneralRegulations().isEmpty())
        {
            List<GeneralRegulation> generalRegulations = request.getGeneralRegulations().stream()
                    .map(generalRegulationRequest ->  GeneralRegulation.builder()
                            .regulationName(generalRegulationRequest.getRegulationName())
                            .regulationContent(generalRegulationRequest.getRegulationContent())
                            .accommodation(savedAccommodation)
                            .build())
                    .toList();
            generalRegulationRepository.saveAll(generalRegulations);
        }
        return ResponseEntity.ok("Tạo nơi cư trú thành công!");
    }

    // Sửa thông tin nơi cư trú
    @Transactional
    public ResponseEntity<String> updateAccommodation(UUID idAccommodation, AccommodationUpdateRequest request)
    {
        Accommodation accommodation = accommodationRepository.findById(idAccommodation).orElseThrow(
                () -> new ApiException(ErrorCode.ACCOMMODATION_NOT_FOUND)
        );

        accommodationMapper.updateAccommodationFromRequest(request, accommodation);
        Accommodation updateAccommodation = accommodationRepository.save(accommodation);

        // Nếu có upload ảnh mới
        if (request.getListImageAccommodation() != null && !request.getListImageAccommodation().isEmpty()) {
            imagesRepository.deleteByRefId(updateAccommodation.getIdAccommodation().toString());

            List<Images> images = request.getListImageAccommodation().stream().map(url -> {
                Images image = new Images();
                image.setUrl(url);
                image.setRefId(updateAccommodation.getIdAccommodation().toString());
                return image;
            }).toList();
            imagesRepository.saveAll(images);
        }

        // Nếu có thay đổi loại tiện ích hoặc tiện ich thì xóa cũ thêm mới
        if (request.getFacilityCategories() != null && !request.getFacilityCategories().isEmpty()) {
            facilityCategoryRepository.deleteByAccommodation_IdAccommodation(idAccommodation);

            List<FacilityCategory> categories = request.getFacilityCategories()
                    .stream()
                    .map(category -> {
                        FacilityCategory facilityCategory = FacilityCategory.builder()
                                .categoryName(category.getCategoryName())
                                .accommodation(updateAccommodation)
                                .build();

                        if (category.getFacilities() != null && !category.getFacilities().isEmpty()) {
                            List<Facility> facilities = category.getFacilities().stream()
                                    .map(facilityRequest -> Facility.builder()
                                            .facilityName(facilityRequest.getFacilityName())
                                            .isFree(facilityRequest.getIsFree())
                                            .category(facilityCategory)
                                            .build())
                                    .toList();
                            facilityCategory.setFacilities(facilities);
                        }
                        return facilityCategory;
                    }).toList();
            facilityCategoryRepository.saveAll(categories);

            // Nếu có thay đổi luật thì xóa list cũ thêm list mới
            if (request.getGeneralRegulations() != null && !request.getGeneralRegulations().isEmpty()) {
                generalRegulationRepository.deleteByAccommodation_IdAccommodation(idAccommodation);

                List<GeneralRegulation> generalRegulations = request.getGeneralRegulations().stream()
                        .map(generalRegulationRequest -> GeneralRegulation.builder()
                                .regulationName(generalRegulationRequest.getRegulationName())
                                .regulationContent(generalRegulationRequest.getRegulationContent())
                                .accommodation(updateAccommodation)
                                .build())
                        .toList();
                generalRegulationRepository.saveAll(generalRegulations);
            }

        }

        return ResponseEntity.ok("Cập nhật thông tin nơi cư trú thành công!");
    }

    // Xóa nơi cư trú
    public ResponseEntity<String> deleteAccommodation(UUID idAccommodation)
    {
        Accommodation accommodation = accommodationRepository.findById(idAccommodation).orElseThrow(
                () -> new ApiException(ErrorCode.ACCOMMODATION_NOT_FOUND)
        );

        if (!userRepository.existsById(accommodation.getUsers().getIdUser()))
            throw new ApiException(ErrorCode.USER_NOT_FOUND);

        imagesRepository.deleteByRefId(idAccommodation.toString());
        favoriteRepository.deleteByRefIdAndRefType(idAccommodation.toString(), PredefinedRefFavoriteAndEvaluate.REF_ACCOMMODATION);
        evaluatesRepository.deleteByRefIdAndRefType(idAccommodation.toString(), PredefinedRefFavoriteAndEvaluate.REF_ACCOMMODATION);
        accommodationRepository.delete(accommodation);
        return ResponseEntity.ok("Xóa nơi cư trú thành công!");
    }
}