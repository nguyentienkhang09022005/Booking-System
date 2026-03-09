package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.TransportTicketApproveRequest;
import com.example.java_service.Dto.Request.TransportTicketCreationRequest;
import com.example.java_service.Dto.Request.TransportTicketUpdateRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.TransportTicketResponse;
import com.example.java_service.Services.TransportTicketService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/transport-ticket")
public class TransportTicketController
{
    TransportTicketService transportTicketService;

    @GetMapping("/get-all")
    public ApiResponse<List<TransportTicketResponse>> getAllTickets()
    {
        return ApiResponse.<List<TransportTicketResponse>>builder()
                .message("Lấy danh sách tất cả vé di chuyển thành công!")
                .data(transportTicketService.getAllTickets())
                .build();
    }

    @GetMapping("/get-list-approved")
    public ApiResponse<List<TransportTicketResponse>> getTicketsApproved()
    {
        return ApiResponse.<List<TransportTicketResponse>>builder()
                .message("Lấy danh sách vé đã phê duyệt thành công!")
                .data(transportTicketService.getListTicketApproved())
                .build();
    }

    @GetMapping("/get-list-not-approved")
    public ApiResponse<List<TransportTicketResponse>> getTicketsNotApproved()
    {
        return ApiResponse.<List<TransportTicketResponse>>builder()
                .message("Lấy danh sách vé chưa phê duyệt thành công!")
                .data(transportTicketService.getListTicketNotApproved())
                .build();
    }

    @GetMapping("/list-of-user")
    public ApiResponse<List<TransportTicketResponse>> getTicketsCreatedByUser(@RequestParam UUID idUser)
    {
        return ApiResponse.<List<TransportTicketResponse>>builder()
                .message("Lấy danh sách vé của người dùng thành công!")
                .data(transportTicketService.getListTicketCreatedByUser(idUser))
                .build();
    }

    @PostMapping("/creation")
    public ApiResponse<ResponseEntity<String>> creationTransportTicket(@RequestBody TransportTicketCreationRequest request)
    {
        var response = transportTicketService.saveTransportTicket(request);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    @PatchMapping("/update")
    public ApiResponse<ResponseEntity<String>> updateTransportTicket(@RequestParam UUID idTicket,
                                                                     @RequestBody TransportTicketUpdateRequest request)
    {
        var response = transportTicketService.updateTransportTicket(idTicket, request);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    @DeleteMapping("/delete")
    public ApiResponse<ResponseEntity<String>> deleteTransportTicket(@RequestParam UUID idTicket)
    {
        var response = transportTicketService.deleteTransportTicket(idTicket);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    @PatchMapping("/approve")
    public ApiResponse<ResponseEntity<String>> approveTransportTicket(@RequestParam UUID idTicket,
                                                                      @RequestBody TransportTicketApproveRequest request)
    {
        var response = transportTicketService.approveTransportTicket(idTicket, request);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }
}