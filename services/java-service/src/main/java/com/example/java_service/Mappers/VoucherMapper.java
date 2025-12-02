package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.VoucherCreationRequest;
import com.example.java_service.Dto.Request.VoucherUpdateRequest;
import com.example.java_service.Dto.Response.VoucherEditResponse;
import com.example.java_service.Dto.Response.VoucherViewAllResponse;
import com.example.java_service.Dto.Response.VoucherViewResponse;
import com.example.java_service.Models.Voucher;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface VoucherMapper
{
    @Mapping(target = "hotel", ignore = true)
    Voucher toVoucher(VoucherCreationRequest request);

    VoucherEditResponse toVoucherEditResponse(Voucher voucher);

    VoucherViewResponse toVoucherViewResponse(Voucher voucher);

    @Mapping(target = "idHotel", source = "hotel.idHotel")
    VoucherViewAllResponse toVoucherViewAllResponse(Voucher voucher);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateVoucherFromRequest(VoucherUpdateRequest request, @MappingTarget Voucher voucher);
}
