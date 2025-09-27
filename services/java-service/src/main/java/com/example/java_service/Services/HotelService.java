package com.example.java_service.Services;

import com.example.java_service.Dto.Request.HotelCreationRequest;
import com.example.java_service.Dto.Request.HotelCheckFavoriteRequest;
import com.example.java_service.Dto.Request.HotelUpdateRequest;
import com.example.java_service.Dto.Response.*;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.EvaluateMapper;
import com.example.java_service.Mappers.HotelMapper;
import com.example.java_service.Mappers.UserMapper;
import com.example.java_service.Models.*;
import com.example.java_service.Repositories.Jpa.*;
import com.example.java_service.Repositories.Mongodb.EvaluatesRepository;
import com.example.java_service.Repositories.Mongodb.FavoriteRepository;
import com.example.java_service.Repositories.Mongodb.ImagesRepository;
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
public class HotelService
{
    UserRepository userRepository;
    HotelRepository hotelRepository;
    FavoriteRepository favoriteRepository;
    ImagesRepository imagesRepository;
    EvaluatesRepository evaluatesRepository;
    FacilityCategoryRepository facilityCategoryRepository;
    GeneralRegulationRepository generalRegulationRepository;
    HotelFacilityRepository hotelFacilityRepository;
    EvaluatesService evaluatesService;
    HotelMapper hotelMapper;
    UserMapper userMapper;
    EvaluateMapper evaluateMapper;

    // Hàm lấy danh sách khách sạn bằng paging 8
    public List<GetListHotelResponse> getListHotel(UUID idUser) {
        List<Hotel> hotelList = hotelRepository.findAll();
        if (hotelList.isEmpty()) {
            return List.of();
        }

        final Set<String> favoriteHotelIds;
        if (idUser != null) {
            Users user = userRepository.findById(idUser).orElseThrow(
                    () -> new ApiException(ErrorCode.USER_NOT_FOUND));
            favoriteHotelIds = favoriteRepository.findByIdUser(user.getIdUser().toString())
                    .stream()
                    .map(Favorite::getIdHotel)
                    .collect(Collectors.toSet());
        } else {
            favoriteHotelIds = Set.of();
        }

        // Lấy ID của tất cả khách sạn
        List<String> hotelIds = hotelList.stream()
                .map(hotel -> hotel.getIdHotel().toString())
                .toList();

        // Lấy tất cả ảnh
        List<Images> images = imagesRepository.findByRefIdIn(hotelIds);
        Map<String, List<String>> hotelImagesMap = images.stream()
                .collect(Collectors.groupingBy(
                        Images::getRefId,
                        Collectors.mapping(Images::getUrl, Collectors.toList())
                ));

        // Lấy thống kê
        Map<String, HotelStatsResponse> statsMap = evaluatesRepository.findHotelStatsByHotelIds(hotelIds)
                .stream()
                .collect(Collectors.toMap(HotelStatsResponse::getIdHotel, dto -> dto));

        return hotelList.stream()
                .map(hotel -> {
                    GetListHotelResponse response = hotelMapper.toGetListHotelResponse(hotel);
                    String hotelIdStr = hotel.getIdHotel().toString();

                    HotelStatsResponse stats = statsMap.get(hotelIdStr);

                    response.setFavorite(favoriteHotelIds.contains(hotelIdStr));
                    response.setImageHotelUrl(hotelImagesMap.get(hotelIdStr));

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

    // Lấy thông tin chi tiết của khách sạn qua id (bổ sung lấy thêm evaluates top 5 gần nhất)
    public GetDetailHotelResponse getHotelDetail(UUID idHotel, UUID idUser)
    {
        Hotel hotel = hotelRepository.findById(idHotel).orElseThrow(
                () -> new ApiException(ErrorCode.HOTEL_NOT_FOUND)
        );

        var favoriteCheck = favoriteRepository.existsByIdUserAndIdHotel(
                idUser.toString(),
                idHotel.toString());
        List<String> imageUrls = imagesRepository.findByRefId(hotel.getIdHotel().toString())
                .stream()
                .map(Images::getUrl)
                .toList();

        OwnerHotelResponse ownerHotelResponse = userMapper.toOwnerHotelResponse(hotel.getUsers());

        // Lấy tất cả đánh giá của khách sạn
        Integer allEvaluate = evaluatesService.getAllEvaluateOfHotel(idHotel);
        // Lấy trung bình số sao của khách sạn
        Double stars = evaluatesService.getAverageStarOfHotel(idHotel);

        // Lấy danh sách đánh giá
        List<Evaluates> top3Evaluates = evaluatesRepository.findTop3ByIdHotelOrderByCreatedAtAsc(idHotel.toString());
        List<EvaluateResponse> evaluateResponses = top3Evaluates.stream()
                .map(evaluateMapper::toEvaluateResponse)
                .toList();

        // Lấy categories + facilities
        List<GetDetailHotelResponse.FacilityCategoryResponse> facilityCategoryResponses =
                facilityCategoryRepository.findDistinctByFacilities_HotelFacilities_Hotel_IdHotel(idHotel)
                        .stream()
                        .map(cat -> GetDetailHotelResponse.FacilityCategoryResponse.builder()
                                .idCategory(cat.getIdCategory())
                                .categoryName(cat.getCategoryName())
                                .facilities(
                                        cat.getFacilities().stream()
                                                .map(fac -> GetDetailHotelResponse.FacilityResponse.builder()
                                                        .idFacility(fac.getIdFacility())
                                                        .facilityName(fac.getFacilityName())
                                                        .isFree(fac.getIsFree())
                                                        .build())
                                                .toList()
                                )
                                .build())
                        .toList();

        // 8. Lấy general regulations
        List<GetDetailHotelResponse.GeneralRegulationResponse> regulationResponses =
                generalRegulationRepository.findByHotel_IdHotel(idHotel)
                        .stream()
                        .map(reg -> GetDetailHotelResponse.GeneralRegulationResponse.builder()
                                .idRegulation(reg.getIdRegulation())
                                .regulationName(reg.getRegulationName())
                                .regulationContent(reg.getRegulationContent())
                                .build())
                        .toList();

        GetDetailHotelResponse hotelResponse = hotelMapper.toGetDetailHotelResponse(hotel);
        hotelResponse.setOwnerHotelResponse(ownerHotelResponse);
        hotelResponse.setImageHotel(imageUrls);
        hotelResponse.setFavorite(favoriteCheck);
        hotelResponse.setEvaluates(evaluateResponses);
        hotelResponse.setAllEvaluates(allEvaluate);
        hotelResponse.setAverageStar(stars);
        hotelResponse.setFacilityCategoryResponses(facilityCategoryResponses);
        hotelResponse.setGeneralRegulationResponses(regulationResponses);
        return hotelResponse;
    }

    // Thêm khách sạn mới
    public HotelResponse saveHotel(HotelCreationRequest request)
    {
        Users user = userRepository.findById(request.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        Hotel hotel = hotelMapper.toHotel(request);
        hotel.setUsers(user);
        Hotel savedHotel = hotelRepository.save(hotel);

        // Lưu ảnh từ URL đã upload
        if (request.getListImageHotel() != null && !request.getListImageHotel().isEmpty()) {
            List<Images> images = request.getListImageHotel().stream().map(url -> {
                Images image = new Images();
                image.setUrl(url);
                image.setRefId(savedHotel.getIdHotel().toString());
                return image;
            }).toList();
            imagesRepository.saveAll(images);
        }

        // Map thông tin khách sạn cơ bản
        OwnerHotelResponse ownerHotelResponse = userMapper.toOwnerHotelResponse(user);
        HotelResponse hotelResponse = hotelMapper.toHotelResponse(savedHotel);
        hotelResponse.setOwnerHotelResponse(ownerHotelResponse);

        if (request.getFacilityCategories() != null && !request.getFacilityCategories().isEmpty())
        {
            List<FacilityCategory> categories = request.getFacilityCategories()
                    .stream()
                    .map(category -> {
                        FacilityCategory facilityCategory = FacilityCategory.builder()
                                .categoryName(category.getCategoryName())
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
            List<FacilityCategory> savedCategories = facilityCategoryRepository.saveAll(categories);


            // Tạo quan hệ HotelFacility
            List<HotelFacility> hotelFacilities = categories.stream()
                    .flatMap(cat -> cat.getFacilities().stream()
                            .map(fac -> HotelFacility.builder()
                                    .idHotelFacility(new HotelFacilityId(savedHotel.getIdHotel(), fac.getIdFacility()))
                                    .hotel(savedHotel)
                                    .facility(fac)
                                    .build()))
                    .toList();
            hotelFacilityRepository.saveAll(hotelFacilities);

            // Map sang response
            List<GetDetailHotelResponse.FacilityCategoryResponse> facilityCategoryResponses =
                    savedCategories.stream()
                            .map(cat -> GetDetailHotelResponse.FacilityCategoryResponse.builder()
                                    .idCategory(cat.getIdCategory())
                                    .categoryName(cat.getCategoryName())
                                    .facilities(cat.getFacilities().stream()
                                            .map(fac -> GetDetailHotelResponse.FacilityResponse.builder()
                                                    .idFacility(fac.getIdFacility())
                                                    .facilityName(fac.getFacilityName())
                                                    .isFree(fac.getIsFree())
                                                    .build())
                                            .toList())
                                    .build())
                            .toList();

            hotelResponse.setFacilityCategoryResponses(facilityCategoryResponses);
        }

        if (request.getGeneralRegulations() != null && !request.getGeneralRegulations().isEmpty())
        {
            List<GeneralRegulation> generalRegulations = request.getGeneralRegulations().stream()
                    .map(generalRegulationRequest ->  GeneralRegulation.builder()
                            .regulationName(generalRegulationRequest.getRegulationName())
                            .regulationContent(generalRegulationRequest.getRegulationContent())
                            .hotel(savedHotel)
                            .build())
                    .toList();
            generalRegulationRepository.saveAll(generalRegulations);

            List<GeneralRegulation> savedRegs = generalRegulationRepository.saveAll(generalRegulations);

            // Map sang response
            List<GetDetailHotelResponse.GeneralRegulationResponse> generalRegulationResponses =
                    savedRegs.stream()
                            .map(reg -> GetDetailHotelResponse.GeneralRegulationResponse.builder()
                                    .idRegulation(reg.getIdRegulation())
                                    .regulationName(reg.getRegulationName())
                                    .regulationContent(reg.getRegulationContent())
                                    .build())
                            .toList();

            hotelResponse.setGeneralRegulationResponses(generalRegulationResponses);
        }

        return hotelResponse;
    }

    // Sửa thông tin khách sạn
    public HotelResponse updateHotel(UUID idHotel, HotelUpdateRequest request)
    {
        Hotel hotel = hotelRepository.findById(idHotel).orElseThrow(
                () -> new ApiException(ErrorCode.HOTEL_NOT_FOUND)
        );

        Users user = userRepository.findById(hotel.getUsers().getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );
        hotelMapper.updateHotelFromRequest(request, hotel);
        Hotel updateHotel = hotelRepository.save(hotel);

        // Nếu có upload ảnh mới
        if (request.getListImageHotel() != null && !request.getListImageHotel().isEmpty()) {
            imagesRepository.deleteByRefId(updateHotel.getIdHotel().toString());

            List<Images> images = request.getListImageHotel().stream().map(url -> {
                Images image = new Images();
                image.setUrl(url);
                image.setRefId(updateHotel.getIdHotel().toString());
                return image;
            }).toList();
            imagesRepository.saveAll(images);
        }
        OwnerHotelResponse ownerHotelResponse = userMapper.toOwnerHotelResponse(user);
        var hotelResponse = hotelMapper.toHotelResponse(updateHotel);
        hotelResponse.setOwnerHotelResponse(ownerHotelResponse);
        return hotelResponse;
    }

    // Xóa khách sạn còn và evaluate
    public ResponseEntity<String> deleteHotel(HotelCheckFavoriteRequest request)
    {
        Hotel hotel = hotelRepository.findById(request.getIdHotel()).orElseThrow(
                () -> new ApiException(ErrorCode.HOTEL_NOT_FOUND)
        );

        if (!userRepository.existsById(hotel.getUsers().getIdUser()))
            throw new ApiException(ErrorCode.USER_NOT_FOUND);

        imagesRepository.deleteByRefId(request.getIdHotel().toString());
        favoriteRepository.deleteByIdUserAndIdHotel(request.getIdUser().toString(), request.getIdHotel().toString());
        hotelRepository.delete(hotel);
        return ResponseEntity.ok("Xóa khách sạn thành công!");
    }
}
