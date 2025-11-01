package com.example.java_service.Services;

import com.example.java_service.Dto.Request.UserVoucherRequest;
import com.example.java_service.Dto.Response.UserVoucherResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.UserVoucherMapper;
import com.example.java_service.Models.*;
import com.example.java_service.Repositories.Jpa.UserRepository;
import com.example.java_service.Repositories.Jpa.UserVoucherRepository;
import com.example.java_service.Repositories.Jpa.VoucherRepository;
import com.example.java_service.Repositories.Mongodb.ImagesRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserVoucherService
{
    UserVoucherRepository userVoucherRepository;
    UserRepository userRepository;
    VoucherRepository voucherRepository;
    ImagesRepository imagesRepository;
    UserVoucherMapper userVoucherMapper;

    private static final int DEFAULT_VOUCHER_QUANTITY_SAVED = 1;

    // Hàm lấy danh sách voucher đã lưu và còn hạn (cho user)
    public List<UserVoucherResponse> listUserVoucherSavedAndValid(UUID idUser)
    {
        userRepository.findById(idUser).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        LocalDate today = LocalDate.now();

        List<UserVoucher> userVouchers = userVoucherRepository.findValidByUserIdAndVoucherEndDateAfterOrEqual(idUser, today);

        // Lấy ID của tất cả voucher
        List<String> voucherIds = userVouchers.stream()
                .map(userVoucher -> userVoucher.getVoucher().getIdVoucher().toString())
                .toList();

        // Lấy tất cả ảnh
        List<Images> images = imagesRepository.findByRefIdIn(voucherIds);
        Map<String, String> voucherImagesMap = images.stream()
                .collect(Collectors.toMap(
                        Images::getRefId,
                        Images::getUrl
                ));

        return userVouchers.stream()
                .map(userVoucher -> {
                    UserVoucherResponse response = userVoucherMapper.toUserVoucherResponse(userVoucher);
                    String voucherIdStr = userVoucher.getVoucher().getIdVoucher().toString();

                    if (response.getVoucherViewResponse() != null) {
                        response.getVoucherViewResponse().setUrlImage(voucherImagesMap.get(voucherIdStr));
                    }
                return response;
                })
                .toList();
    }

    // Hàm lưu voucher (cho user)
    @Transactional
    public ResponseEntity<String> saveUserVoucher(UserVoucherRequest userVoucherRequest)
    {
        Users user = userRepository.findById(userVoucherRequest.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        Voucher voucher = voucherRepository.findById(userVoucherRequest.getIdVoucher()).orElseThrow(
                () -> new ApiException(ErrorCode.VOUCHER_NOT_FOUND)
        );

        UserVoucherId userVoucherId = new UserVoucherId(user.getIdUser(), voucher.getIdVoucher());

        // Kiểm tra voucher đã được lưu chưa
        if (userVoucherRepository.existsById(userVoucherId)) {
            throw new ApiException(ErrorCode.VOUCHER_ALREADY_SAVED);
        }

        // Xữ lý race condition bằng Atomistic
        UUID idVoucher = userVoucherRequest.getIdVoucher();

        int voucherUpdated = voucherRepository.decreaseVoucherQuantity(idVoucher, DEFAULT_VOUCHER_QUANTITY_SAVED);
        if (voucherUpdated == 0)
            throw new ApiException(ErrorCode.VOUCHER_NOT_ENOUGH_QUANTITY);

        UserVoucher userVoucher = UserVoucher.builder()
                .user(user)
                .voucher(voucher)
                .isUsed(false)
                .usedAt(null)
                .build();

        userVoucherRepository.save(userVoucher);

        return ResponseEntity.ok("Lưu voucher thành công!");
    }
}
