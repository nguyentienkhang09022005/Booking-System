package com.example.java_service.Services;

import com.example.java_service.Configs.RabbitConfig;
import com.example.java_service.Dto.Request.ExpenseCreationRequest;
import com.example.java_service.Dto.Request.ExpenseUpdateRequest;
import com.example.java_service.Dto.Response.ExpenseResponse;
import com.example.java_service.Dto.Response.InfUserCreatedExpenseResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.ExpenseMapper;
import com.example.java_service.Models.*;
import com.example.java_service.Repositories.Jpa.ExpenseRepository;
import com.example.java_service.Repositories.Jpa.HotelRepository;
import com.example.java_service.Repositories.Jpa.UserRepository;
import com.example.java_service.Repositories.Mongodb.ImagesRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExpenseService
{
    ExpenseRepository expenseRepository;
    HotelRepository hotelRepository;
    UserRepository userRepository;
    ImagesRepository imagesRepository;
    ExpenseMapper expenseMapper;
    ObjectMapper objectMapper;
    RabbitTemplate rabbitTemplate;
    RabbitConfig rabbitConfig;

    // Hàm lấy danh sách chi tiêu của khách sạn
    public List<ExpenseResponse> getListExpense(UUID idHotel)
    {
        hotelRepository.findById(idHotel).orElseThrow(
                () -> new ApiException(ErrorCode.HOTEL_NOT_FOUND)
        );

        List<Expense> expenseList = expenseRepository.findByHotel_IdHotel(idHotel);
        if (expenseList.isEmpty()) {
            return List.of();
        }

        return expenseList.stream()
                .map(expense -> {
                    ExpenseResponse expenseResponse = expenseMapper.toExpenseResponse(expense);

                    String avatarUrl = imagesRepository.findByRefId(expense.getUser().getIdUser().toString())
                            .stream()
                            .map(Images::getUrl)
                            .findFirst()
                            .orElse(null);

                    InfUserCreatedExpenseResponse infUserCreatedExpenseResponse = InfUserCreatedExpenseResponse.builder()
                            .idUser(expense.getUser().getIdUser())
                            .firstName(expense.getUser().getFirstName())
                            .lastName(expense.getUser().getLastName())
                            .avatar(avatarUrl)
                            .build();

                    expenseResponse.setInfUserCreatedExpenseResponse(infUserCreatedExpenseResponse);
                    return expenseResponse;
                }).toList();
    }

    // Hàm lấy thông tin chi tiêu
    public ExpenseResponse getInfExpense(UUID idExpense)
    {
        Expense expense = expenseRepository.findById(idExpense).orElseThrow(
                () -> new ApiException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        ExpenseResponse expenseResponse = expenseMapper.toExpenseResponse(expense);
        String avatarUrl = imagesRepository.findByRefId(expense.getUser().getIdUser().toString())
                .stream()
                .map(Images::getUrl)
                .findFirst()
                .orElse(null);

        InfUserCreatedExpenseResponse infUserCreatedExpenseResponse = InfUserCreatedExpenseResponse.builder()
                .idUser(expense.getUser().getIdUser())
                .firstName(expense.getUser().getFirstName())
                .lastName(expense.getUser().getLastName())
                .avatar(avatarUrl)
                .build();

        expenseResponse.setInfUserCreatedExpenseResponse(infUserCreatedExpenseResponse);
        return expenseResponse;
    }

    // Hàm thêm chi tiêu mới
    public ExpenseResponse saveExpense(ExpenseCreationRequest request)
    {
        Users user = userRepository.findById(request.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        Hotel hotel = hotelRepository.findById(request.getIdHotel()).orElseThrow(
                () -> new ApiException(ErrorCode.HOTEL_NOT_FOUND)
        );

        if (request.getAmount().compareTo(BigDecimal.ZERO) < 0){
            throw new ApiException(ErrorCode.INVALID_AMOUNT);
        }

        Expense expense = expenseMapper.toExpense(request);
        expense.setHotel(hotel);
        expense.setUser(user);

        Expense savedExpense = expenseRepository.save(expense);
//        try
//        {
//            String message = objectMapper.writeValueAsString(savedExpense);
//            rabbitTemplate.convertAndSend(
//                    rabbitConfig.EXCHANGE_NAME,
//                    rabbitConfig.ROUTING_KEY,
//                    message
//            );
//        } catch (Exception ex)
//        {
//            ex.printStackTrace();
//        }

        ExpenseResponse expenseResponse = expenseMapper.toExpenseResponse(savedExpense);
        String avatarUrl = imagesRepository.findByRefId(user.getIdUser().toString())
                .stream()
                .map(Images::getUrl)
                .findFirst()
                .orElse(null);

        InfUserCreatedExpenseResponse infUserCreatedExpenseResponse = InfUserCreatedExpenseResponse.builder()
                .idUser(user.getIdUser())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatar(avatarUrl)
                .build();

        expenseResponse.setInfUserCreatedExpenseResponse(infUserCreatedExpenseResponse);
        return expenseResponse;
    }

    //  Hàm xóa chi tiêu
    public ResponseEntity<String> deleteExpense(UUID idExpense)
    {
        Expense expense = expenseRepository.findById(idExpense).orElseThrow(
                () -> new ApiException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        expenseRepository.delete(expense);
        return ResponseEntity.ok("Xóa chi tiêu thành công!");
    }

    // Hàm thay đổi thông tin chi tiêu
    public ExpenseResponse updateExpense(ExpenseUpdateRequest request, UUID idExpense)
    {
        Expense expense = expenseRepository.findById(idExpense).orElseThrow(
                () -> new ApiException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        if (request.getAmount().compareTo(BigDecimal.ZERO) < 0){
            throw new ApiException(ErrorCode.INVALID_AMOUNT);
        }

        expenseMapper.updateExpenseFromRequest(request, expense);

        Expense updatedExpense = expenseRepository.save(expense);

        ExpenseResponse expenseResponse = expenseMapper.toExpenseResponse(updatedExpense);
        String avatarUrl = imagesRepository.findByRefId(updatedExpense.getUser().getIdUser().toString())
                .stream()
                .map(Images::getUrl)
                .findFirst()
                .orElse(null);

        InfUserCreatedExpenseResponse infUserCreatedExpenseResponse = InfUserCreatedExpenseResponse.builder()
                .idUser(updatedExpense.getUser().getIdUser())
                .firstName(updatedExpense.getUser().getFirstName())
                .lastName(updatedExpense.getUser().getLastName())
                .avatar(avatarUrl)
                .build();

        expenseResponse.setInfUserCreatedExpenseResponse(infUserCreatedExpenseResponse);
        return expenseResponse;
    }
}
