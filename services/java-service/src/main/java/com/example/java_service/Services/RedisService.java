package com.example.java_service.Services;

import com.example.java_service.Dto.Response.GetDetailHotelResponse;
import com.example.java_service.Dto.Response.GetListHotelResponse;
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
    HotelService hotelService;
    ServiceService serviceService;
    VoucherService voucherService;

    static final String PREFIX_HOTEL = "hotel:";
    static final String PREFIX_DETAIL_HOTEL = "detail_hotel:";
    static final String PREFIX_SERVICE = "service:";
    static final String PREFIX_VOUCHER = "voucher:";

    ObjectMapper objectMapper;

    private String getKeyHotel(String idUser) {
        return PREFIX_HOTEL + idUser;
    }
    private String getKeyDetailHotel(String idUser) {
        return PREFIX_DETAIL_HOTEL + idUser;
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

    public List<GetListHotelResponse> getHotelsCached(UUID idUser) {
        return getOrSetList(
                getKeyHotel(idUser.toString()),
                86400,
                new TypeReference<List<GetListHotelResponse>>() {},
                () -> hotelService.getListHotelApproved(idUser)
        );
    }

    public GetDetailHotelResponse getHotelDetailCached(UUID idHotel, UUID idUser) {
        return getOrSet(
                getKeyDetailHotel(idHotel.toString()),
                86400,
                GetDetailHotelResponse.class,
                () -> hotelService.getHotelDetail(idHotel, idUser)
        );
    }

    public List<ServiceResponse> getServicesCached(UUID idHotel) {
        return getOrSetList(
                getKeyService(idHotel.toString()),
                21600,
                new TypeReference<List<ServiceResponse>>() {},
                () -> serviceService.getListService(idHotel)
        );
    }

    public List<VoucherViewResponse> getVouchersCached(UUID idHotel) {
        return getOrSetList(
                getKeyVoucher(idHotel.toString()),
                60,
                new TypeReference<List<VoucherViewResponse>>() {},
                () -> voucherService.listVouchersApprovedByHotel(idHotel)
        );
    }
}
