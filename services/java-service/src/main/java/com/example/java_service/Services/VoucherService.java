package com.example.java_service.Services;

import com.example.java_service.Dto.Request.VoucherCreationRequest;
import com.example.java_service.Dto.Request.VoucherUpdateRequest;
import com.example.java_service.Dto.Response.VoucherEditResponse;
import com.example.java_service.Dto.Response.VoucherViewAllResponse;
import com.example.java_service.Dto.Response.VoucherViewResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.VoucherMapper;
import com.example.java_service.Models.Hotel;
import com.example.java_service.Models.Images;
import com.example.java_service.Models.Voucher;
import com.example.java_service.Repositories.Jpa.HotelRepository;
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
    HotelRepository hotelRepository;
    ImagesRepository imagesRepository;
    VoucherMapper voucherMapper;

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

    // Lấy danh sách voucher của khách sạn đã được phê duyệt và còn hạn (cho user)
    public List<VoucherViewResponse> listVouchersApprovedByHotel(UUID idHotel, UUID idUser)
    {
        if (!hotelRepository.existsByIdHotel(idHotel)){
            throw new ApiException(ErrorCode.HOTEL_NOT_FOUND);
        }

        if (!userRepository.existsByIdUser(idUser)){
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        List<Voucher> vouchers = voucherRepository.findVoucherByApprovedTrueAndHotel_IdHotel(idHotel);

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
                    VoucherViewResponse response = voucherMapper.toVoucherViewResponse(voucher);
                    String voucherIdStr = voucher.getIdVoucher().toString();

                    response.setIsSaved(savedVoucherSet.contains(voucher.getIdVoucher()));
                    response.setUrlImage(voucherImagesMap.get(voucherIdStr));

                    return response;
                })
                .toList();
    }

    // Lấy danh sách voucher
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

    // Lấy danh sách voucher của khách sạn (cho company)
    public List<VoucherEditResponse> listVouchersOfHotel(UUID idHotel)
    {
        if (!hotelRepository.existsByIdHotel(idHotel)){
            throw new ApiException(ErrorCode.HOTEL_NOT_FOUND);
        }

        List<Voucher> vouchers = voucherRepository.findVoucherByHotel_IdHotel(idHotel);

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
    public VoucherViewResponse saveVoucher(VoucherCreationRequest voucherCreationRequest)
    {
        // ---------Validate Input---------
        // Kiểm tra ngày kết thúc của voucher không nằm trong quá khứ
        if (voucherCreationRequest.getEndDate().isBefore(voucherCreationRequest.getStartDate())){
            throw new ApiException(ErrorCode.END_DATE_BEFORE_STAR_DATE);
        }

        // Kiểm tra số lượng phải lớn hơn 0
        if (voucherCreationRequest.getQuantity() < 0){
            throw new ApiException(ErrorCode.QUANTITY_NOT_ENOUGH);
        }

        // Kiểm tra phần trăm discount lớn hơn 0
        if (voucherCreationRequest.getDiscountPercent().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorCode.PERCENT_NOT_NEGATIVE);
        }

        Hotel hotel = hotelRepository.findById(voucherCreationRequest.getIdHotel()).orElseThrow(
                () -> new ApiException(ErrorCode.HOTEL_NOT_FOUND)
        );

        Voucher voucher = voucherMapper.toVoucher(voucherCreationRequest);
        voucher.setHotel(hotel);
        voucher.setCode(generateUniqueVoucherCode());
        voucher.setApproved(false);
        Voucher savedVoucher = voucherRepository.save(voucher);

        if (voucherCreationRequest.getUrlImage() != null && !voucherCreationRequest.getUrlImage().isEmpty()) {
            Images image = new Images();
            image.setUrl(voucherCreationRequest.getUrlImage());
            image.setRefId(savedVoucher.getIdVoucher().toString());
            imagesRepository.save(image);
        }

        VoucherViewResponse voucherViewResponse = voucherMapper.toVoucherViewResponse(savedVoucher);
        voucherViewResponse.setUrlImage(voucherCreationRequest.getUrlImage());

        return voucherViewResponse;
    }

    // Sửa thông tin của voucher
    public VoucherViewResponse updateVoucher(VoucherUpdateRequest voucherUpdateRequest, UUID idVoucher)
    {
        // ---------Validate Input---------
        // Kiểm tra voucher tồn tại
        Voucher voucher = voucherRepository.findById(idVoucher).orElseThrow(
                () -> new ApiException(ErrorCode.VOUCHER_NOT_FOUND)
        );

        // Kiểm tra ngày kết thúc của voucher không nằm trong quá khứ
        if (voucherUpdateRequest.getStartDate() != null && voucherUpdateRequest.getEndDate() != null) {
            if (voucherUpdateRequest.getEndDate().isBefore(voucherUpdateRequest.getStartDate())) {
                throw new ApiException(ErrorCode.END_DATE_BEFORE_STAR_DATE);
            }
        }

        // Kiểm tra số lượng phải lớn hơn 0
        if (voucherUpdateRequest.getQuantity() != null) {
            if (voucherUpdateRequest.getQuantity() < 0){
                throw new ApiException(ErrorCode.QUANTITY_NOT_ENOUGH);
            }
        }

        // Kiểm tra phần trăm discount lớn hơn 0
        if (voucherUpdateRequest.getDiscountPercent() != null) {
            if (voucherUpdateRequest.getDiscountPercent().compareTo(BigDecimal.ZERO) < 0) {
                throw new ApiException(ErrorCode.PERCENT_NOT_NEGATIVE);
            }
        }

        voucherMapper.updateVoucherFromRequest(voucherUpdateRequest, voucher);

        // Nếu có up load ảnh mới
        if (voucherUpdateRequest.getUrlImage() != null && !voucherUpdateRequest.getUrlImage().isEmpty())
        {
            imagesRepository.deleteByRefId(idVoucher.toString());

            Images image = new Images();
            image.setUrl(voucherUpdateRequest.getUrlImage());
            image.setRefId(idVoucher.toString());
            imagesRepository.save(image);
        }

        return voucherMapper.toVoucherViewResponse(voucherRepository.save(voucher));
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
