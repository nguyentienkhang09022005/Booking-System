package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.ExpenseCreationRequest;
import com.example.java_service.Dto.Request.ExpenseUpdateRequest;
import com.example.java_service.Dto.Response.ExpenseResponse;
import com.example.java_service.Models.Expense;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ExpenseMapper
{
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "user", ignore = true)
    Expense toExpense(ExpenseCreationRequest request);

    @Mapping(target = "infUserCreatedExpenseResponse", ignore = true)
    ExpenseResponse toExpenseResponse(Expense expense);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateExpenseFromRequest(ExpenseUpdateRequest request, @MappingTarget Expense expense);
}
