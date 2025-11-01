package com.example.java_service.Mappers;

import com.example.java_service.Dto.Response.AppliedVoucherResponse;
import com.example.java_service.Models.AppliedVoucher;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AppliedVoucherMapper
{
    AppliedVoucherResponse toAppliedVoucherResponse(AppliedVoucher appliedVoucher);
}
