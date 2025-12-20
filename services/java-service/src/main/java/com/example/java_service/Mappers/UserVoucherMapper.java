package com.example.java_service.Mappers;

import com.example.java_service.Dto.Response.UserVoucherResponse;
import com.example.java_service.Models.UserVoucher;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {VoucherMapper.class})
public interface UserVoucherMapper
{
    @Mapping(source = "voucher", target = "voucherViewResponse")
    UserVoucherResponse toUserVoucherResponse(UserVoucher voucher);
}
