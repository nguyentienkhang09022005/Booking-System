package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedSponsorType;
import com.example.java_service.Dto.Request.BusinessVoucherCreationRequest;
import com.example.java_service.Dto.Request.SystemVoucherCreationRequest;
import com.example.java_service.Dto.Request.VoucherUpdateRequest;
import com.example.java_service.Dto.Response.VoucherEditResponse;
import com.example.java_service.Dto.Response.VoucherViewAllResponse;
import com.example.java_service.Dto.Response.VoucherViewResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.VoucherMapper;
import com.example.java_service.Models.Accommodation;
import com.example.java_service.Models.Images;
import com.example.java_service.Models.Voucher;
import com.example.java_service.Repositories.Jpa.AccommodationRepository;
import com.example.java_service.Repositories.Jpa.UserRepository;
import com.example.java_service.Repositories.Jpa.UserVoucherRepository;
import com.example.java_service.Repositories.Jpa.VoucherRepository;
import com.example.java_service.Repositories.Mongodb.ImagesRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoucherService
{
    VoucherRepository voucherRepository;
    UserRepository userRepository;
    UserVoucherRepository userVoucherRepository;
    AccommodationRepository accommodationRepository;
    ImagesRepository imagesRepository;
    VoucherMapper voucherMapper;
    RagCacheService ragCacheService;

    private static final String CACHE_TYPE = "voucher";
    private static final long CACHE_TTL_SECONDS = 300;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    // Tạo code ngẫu nhiên
    private String generateRandomCode(int minLength, int maxLength) {
        int length = minLength + (int) (Math.random() * (maxLength - minLength + 1));
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    // Tạo code tránh trùng lặp
    private String generateUniqueVoucherCode() {
        String code;
        do {
            code = generateRandomCode(6, 9);
        } while (voucherRepository.existsVoucherByCode(code));
        return code;
    }

    private void validateVoucherInputs(LocalDate startDate, LocalDate endDate, Integer quantity,
                                       BigDecimal discountPercent, BigDecimal minDiscountAmount, BigDecimal maxDiscountAmount) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)){
            throw new ApiException(ErrorCode.END_DATE_BEFORE_STAR_DATE);
        }
        if (quantity != null && quantity < 0){
            throw new ApiException(ErrorCode.QUANTITY_NOT_ENOUGH);
        }
        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorCode.PERCENT_NOT_NEGATIVE);
        }
        if (minDiscountAmount != null && minDiscountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorCode.DISCOUNT_AMOUNT_NOT_NEGATIVE);
        }
        if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorCode.DISCOUNT_AMOUNT_NOT_NEGATIVE);
        }
        if (minDiscountAmount != null && maxDiscountAmount != null && minDiscountAmount.compareTo(maxDiscountAmount) > 0) {
            throw new ApiException(ErrorCode.INVALID_DISCOUNT_AMOUNT);
        }
    }

    // Lấy danh sách voucher của nơi cư trú đã được phê duyệt và còn hạn (cho user)
    public List<VoucherViewResponse> listVouchersApprovedByAccommodation(UUID idAccommodation, UUID idUser)
    {
        if (!accommodationRepository.existsByIdAccommodation(idAccommodation)){
            throw new ApiException(ErrorCode.ACCOMMODATION_NOT_FOUND);
        }

        if (!userRepository.existsByIdUser(idUser)){
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        List<Voucher> vouchers = voucherRepository.findApprovedVouchersForAccommodationAndSystem(idAccommodation);

        if (vouchers.isEmpty()) {
            return List.of();
        }

        List<UUID> voucherUUIDs = vouchers.stream().map(Voucher::getIdVoucher).toList();
        List<String> voucherIdStrings = voucherUUIDs.stream().map(UUID::toString).toList();

        List<Images> images = imagesRepository.findByRefIdIn(voucherIdStrings);
        Map<String, String> voucherImagesMap = images.stream()
                .collect(Collectors.toMap(Images::getRefId, Images::getUrl));

        List<UUID> savedVoucherIds = userVoucherRepository.findSavedVoucherIdsByUserAndVoucherIds(idUser, voucherUUIDs);
        Set<UUID> savedVoucherSet = new HashSet<>(savedVoucherIds);

        return vouchers.stream()
                .map(voucher -> {
                    VoucherViewResponse response = voucherMapper.toVoucherViewResponse(voucher);
                    String voucherIdStr = voucher.getIdVoucher().toString();
                    response.setIsSaved(savedVoucherSet.contains(voucher.getIdVoucher()));
                    response.setUrlImage(voucherImagesMap.get(voucherIdStr));
                    return response;
                })
                .toList();
    }

    public List<VoucherViewAllResponse> listVouchersApproved(UUID idUser)
    {
        if (!userRepository.existsByIdUser(idUser)){
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        List<Voucher> vouchers = voucherRepository.findAll();

        if (vouchers.isEmpty()) {
            return List.of();
        }

        // Lấy ID của tất cả voucher
        List<UUID> voucherUUIDs = vouchers.stream()
                .map(Voucher::getIdVoucher)
                .toList();

        List<String> voucherIdStrings = voucherUUIDs.stream()
                .map(UUID::toString)
                .toList();

        // Lấy tất cả ảnh
        List<Images> images = imagesRepository.findByRefIdIn(voucherIdStrings);
        Map<String, String> voucherImagesMap = images.stream()
                .collect(Collectors.toMap(
                        Images::getRefId,
                        Images::getUrl
                ));

        List<UUID> savedVoucherIds = userVoucherRepository.findSavedVoucherIdsByUserAndVoucherIds(idUser, voucherUUIDs);
        Set<UUID> savedVoucherSet = new HashSet<>(savedVoucherIds);

        return vouchers.stream()
                .map(voucher -> {
                    VoucherViewAllResponse response = voucherMapper.toVoucherViewAllResponse(voucher);
                    String voucherIdStr = voucher.getIdVoucher().toString();

                    response.setIsSaved(savedVoucherSet.contains(voucher.getIdVoucher()));
                    response.setUrlImage(voucherImagesMap.get(voucherIdStr));

                    return response;
                })
                .toList();
    }

    public List<VoucherViewAllResponse> searchVouchers(String keyword)
    {
        String cacheKey = "search:" + keyword.toLowerCase().trim();
        List<VoucherViewAllResponse> cached = ragCacheService.getList(CACHE_TYPE, cacheKey,
                new com.fasterxml.jackson.core.type.TypeReference<List<VoucherViewAllResponse>>() {});

        if (cached != null) {
            log.debug("Cache hit for voucher search: {}", keyword);
            return cached;
        }

        log.debug("Cache miss for voucher search: {}", keyword);
        List<Voucher> vouchers = voucherRepository.findAll();
        String lowerKeyword = keyword.toLowerCase();

        List<VoucherViewAllResponse> result = vouchers.stream()
                .filter(v -> v.getApproved() && (
                        v.getNameVoucher().toLowerCase().contains(lowerKeyword) ||
                        (v.getCode() != null && v.getCode().toLowerCase().contains(lowerKeyword)) ||
                        (v.getConditions() != null && v.getConditions().toLowerCase().contains(lowerKeyword))))
                .map(voucher -> {
                    VoucherViewAllResponse response = voucherMapper.toVoucherViewAllResponse(voucher);
                    response.setIsSaved(false);
                    return response;
                })
                .toList();

        ragCacheService.set(CACHE_TYPE, cacheKey, result, CACHE_TTL_SECONDS);
        return result;
    }

    // Lấy danh sách voucher chưa phê duyệt (cho admin)
    public List<VoucherEditResponse> listVouchersNotApproved()
    {
        List<Voucher> vouchers = voucherRepository.findVoucherByApprovedFalse();

        if (vouchers.isEmpty()) {
            return List.of();
        }

        // Lấy ID của tất cả voucher
        List<String> voucherIds = vouchers.stream()
                .map(voucher -> voucher.getIdVoucher().toString())
                .toList();

        // Lấy tất cả ảnh
        List<Images> images = imagesRepository.findByRefIdIn(voucherIds);
        Map<String, String> voucherImagesMap = images.stream()
                .collect(Collectors.toMap(
                        Images::getRefId,
                        Images::getUrl
                ));

        return vouchers.stream()
                .map(voucher -> {
                    VoucherEditResponse response = voucherMapper.toVoucherEditResponse(voucher);
                    String voucherIdStr = voucher.getIdVoucher().toString();

                    response.setUrlImage(voucherImagesMap.get(voucherIdStr));

                    return response;
                })
                .toList();
    }

    // Lấy danh sách voucher của nơi cư trú (cho company)
    public List<VoucherEditResponse> listVouchersOfAccommodation(UUID idAccommodation)
    {
        if (!accommodationRepository.existsByIdAccommodation(idAccommodation)){
            throw new ApiException(ErrorCode.ACCOMMODATION_NOT_FOUND);
        }

        List<Voucher> vouchers = voucherRepository.findVoucherByAccommodation_IdAccommodation(idAccommodation);

        if (vouchers.isEmpty()) {
            return List.of();
        }

        // Lấy ID của tất cả voucher
        List<String> voucherIds = vouchers.stream()
                .map(voucher -> voucher.getIdVoucher().toString())
                .toList();

        // Lấy tất cả ảnh
        List<Images> images = imagesRepository.findByRefIdIn(voucherIds);
        Map<String, String> voucherImagesMap = images.stream()
                .collect(Collectors.toMap(
                        Images::getRefId,
                        Images::getUrl
                ));

        return vouchers.stream()
                .map(voucher -> {
                    VoucherEditResponse response = voucherMapper.toVoucherEditResponse(voucher);
                    String voucherIdStr = voucher.getIdVoucher().toString();

                    response.setUrlImage(voucherImagesMap.get(voucherIdStr));

                    return response;
                })
                .toList();
    }

    // Tạo voucher
    public VoucherViewResponse saveBusinessVoucher(BusinessVoucherCreationRequest request)
    {
        validateVoucherInputs(request.getStartDate(), request.getEndDate(), request.getQuantity(),
                request.getDiscountPercent(), request.getMinDiscountAmount(), request.getMaxDiscountAmount());

        if (request.getIdAccommodation() == null) {
            throw new ApiException(ErrorCode.ACCOMMODATION_NOT_FOUND);
        }

        Accommodation accommodation = accommodationRepository.findById(request.getIdAccommodation()).orElseThrow(
                () -> new ApiException(ErrorCode.ACCOMMODATION_NOT_FOUND)
        );

        Voucher voucher = voucherMapper.toVoucher(request);
        voucher.setCode(generateUniqueVoucherCode());
        voucher.setSponsorType(PredefinedSponsorType.BUSINESS_TYPE);
        voucher.setAccommodation(accommodation);
        voucher.setApproved(false);

        Voucher savedVoucher = voucherRepository.save(voucher);
        saveVoucherImage(request.getUrlImage(), savedVoucher.getIdVoucher().toString());

        VoucherViewResponse response = voucherMapper.toVoucherViewResponse(savedVoucher);
        response.setUrlImage(request.getUrlImage());
        return response;
    }

    public VoucherViewResponse updateBusinessVoucher(VoucherUpdateRequest request, UUID idVoucher)
    {
        Voucher voucher = voucherRepository.findById(idVoucher).orElseThrow(
                () -> new ApiException(ErrorCode.VOUCHER_NOT_FOUND)
        );

        if (PredefinedSponsorType.SYSTEM_TYPE.equals(voucher.getSponsorType())) {
            throw new ApiException(ErrorCode.SYSTEM_VOUCHER_MODIFICATION_DENIED);
        }

        validateVoucherInputs(request.getStartDate(), request.getEndDate(), request.getQuantity(),
                request.getDiscountPercent(), request.getMinDiscountAmount(), request.getMaxDiscountAmount());

        voucherMapper.updateVoucherFromRequest(request, voucher);
        updateVoucherImage(request.getUrlImage(), idVoucher.toString());

        return voucherMapper.toVoucherViewResponse(voucherRepository.save(voucher));
    }


    public VoucherViewResponse saveSystemVoucher(SystemVoucherCreationRequest request)
    {
        validateVoucherInputs(request.getStartDate(), request.getEndDate(), request.getQuantity(),
                request.getDiscountPercent(), request.getMinDiscountAmount(), request.getMaxDiscountAmount());

        Voucher voucher = voucherMapper.toVoucher(request);
        voucher.setCode(generateUniqueVoucherCode());
        voucher.setSponsorType(PredefinedSponsorType.SYSTEM_TYPE);
        voucher.setAccommodation(null);
        voucher.setApproved(true);

        Voucher savedVoucher = voucherRepository.save(voucher);
        saveVoucherImage(request.getUrlImage(), savedVoucher.getIdVoucher().toString());

        VoucherViewResponse response = voucherMapper.toVoucherViewResponse(savedVoucher);
        response.setUrlImage(request.getUrlImage());
        return response;
    }

    public VoucherViewResponse updateSystemVoucher(VoucherUpdateRequest request, UUID idVoucher)
    {
        Voucher voucher = voucherRepository.findById(idVoucher).orElseThrow(
                () -> new ApiException(ErrorCode.VOUCHER_NOT_FOUND)
        );

        if (PredefinedSponsorType.BUSINESS_TYPE.equals(voucher.getSponsorType())) {
            throw new ApiException(ErrorCode.BUSINESS_VOUCHER_MODIFICATION_DENIED);
        }

        validateVoucherInputs(request.getStartDate(), request.getEndDate(), request.getQuantity(),
                request.getDiscountPercent(), request.getMinDiscountAmount(), request.getMaxDiscountAmount());

        voucherMapper.updateVoucherFromRequest(request, voucher);
        updateVoucherImage(request.getUrlImage(), idVoucher.toString());

        return voucherMapper.toVoucherViewResponse(voucherRepository.save(voucher));
    }

    private void saveVoucherImage(String urlImage, String refId) {
        if (urlImage != null && !urlImage.isEmpty()) {
            Images image = new Images();
            image.setUrl(urlImage);
            image.setRefId(refId);
            imagesRepository.save(image);
        }
    }

    private void updateVoucherImage(String urlImage, String refId) {
        if (urlImage != null && !urlImage.isEmpty()) {
            imagesRepository.deleteByRefId(refId);
            Images image = new Images();
            image.setUrl(urlImage);
            image.setRefId(refId);
            imagesRepository.save(image);
        }
    }

    // Xóa voucher
    public ResponseEntity<String> deleteVoucher(UUID idVoucher)
    {
        Voucher voucher = voucherRepository.findById(idVoucher).orElseThrow(
                () -> new ApiException(ErrorCode.VOUCHER_NOT_FOUND)
        );
        voucherRepository.delete(voucher);
        imagesRepository.deleteByRefId(voucher.getIdVoucher().toString());

        return ResponseEntity.ok("Xóa voucher thành công!");
    }
}
