package com.example.java_service.Services;

import com.example.java_service.Dto.Response.GetDetailAccommodationResponse;
import com.example.java_service.Dto.Response.GetListAccommodationResponse;
import com.example.java_service.Dto.Response.ServiceResponse;
import com.example.java_service.Dto.Response.VoucherViewResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisService
{
    final RedisTemplate<String, String> redisTemplate;
    AccommodationService accommodationService;
    ServiceService serviceService;
    VoucherService voucherService;

    static final String PREFIX_ACCOMMODATION = "accommodation:";
    static final String PREFIX_DETAIL_ACCOMMODATION = "detail_accommodation:";
    static final String PREFIX_SERVICE = "service:";
    static final String PREFIX_VOUCHER = "voucher:";

    ObjectMapper objectMapper;

    private String getKeyAccommodation(String idUser) {
        return PREFIX_ACCOMMODATION + idUser;
    }
    private String getKeyDetailAccommodation(String idUser) {
        return PREFIX_DETAIL_ACCOMMODATION + idUser;
    }
    private String getKeyService(String idUser) {
        return PREFIX_SERVICE + idUser;
    }
    private String getKeyVoucher(String idUser) { return PREFIX_VOUCHER + idUser; }

    public <T> T getOrSet(String key,  long ttlSeconds, Class<T> clazz,  Supplier<T> dbQueryFunction) {
        try {
            String json = redisTemplate.opsForValue().get(key);

            if (json != null && !json.isEmpty()) {
                return objectMapper.readValue(json, clazz);
            }

            T data = dbQueryFunction.get();

            // Lưu Redis
            redisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(data),
                    ttlSeconds,
                    TimeUnit.SECONDS
            );
            return data;

        } catch (Exception e) {
            return dbQueryFunction.get(); // fallback
        }
    }

    public <T> List<T> getOrSetList(String key, long ttlSeconds, TypeReference<List<T>> typeRef, Supplier<List<T>> dbQueryFunction) {
        try {
            String json = redisTemplate.opsForValue().get(key);

            if (json != null && !json.isEmpty()) {
                return objectMapper.readValue(json, typeRef);
            }

            List<T> data = dbQueryFunction.get();
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(data), ttlSeconds, TimeUnit.SECONDS);
            return data;

        } catch (Exception e) {
            return dbQueryFunction.get();
        }
    }

    public List<GetListAccommodationResponse> getAccommodationsCached(UUID idUser) {
        return getOrSetList(
                getKeyAccommodation(idUser.toString()),
                86400,
                new TypeReference<List<GetListAccommodationResponse>>() {},
                () -> accommodationService.getListAccommodationApproved(idUser)
        );
    }

    public GetDetailAccommodationResponse getAccommodationDetailCached(UUID idAccommodation, UUID idUser) {
        return getOrSet(
                getKeyDetailAccommodation(idAccommodation.toString()),
                86400,
                GetDetailAccommodationResponse.class,
                () -> accommodationService.getAccommodationDetail(idAccommodation, idUser)
        );
    }

    public List<ServiceResponse> getServicesCached(UUID idAccommodation) {
        return getOrSetList(
                getKeyService(idAccommodation.toString()),
                21600,
                new TypeReference<List<ServiceResponse>>() {},
                () -> serviceService.getListService(idAccommodation)
        );
    }

    public List<VoucherViewResponse> getVouchersCached(UUID idAccommodation, UUID idUser) {
        return getOrSetList(
                getKeyVoucher(idAccommodation.toString()),
                60,
                new TypeReference<List<VoucherViewResponse>>() {},
                () -> voucherService.listVouchersApprovedByAccommodation(idAccommodation, idUser)
        );
    }
}
