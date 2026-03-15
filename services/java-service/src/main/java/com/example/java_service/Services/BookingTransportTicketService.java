package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedStatusTransportTicket;
import com.example.java_service.Constant.PredefinedTypeTransportTicket;
import com.example.java_service.Dto.Request.TransportTicketApproveRequest;
import com.example.java_service.Dto.Request.TransportTicketCreationRequest;
import com.example.java_service.Dto.Request.TransportTicketUpdateRequest;
import com.example.java_service.Dto.Response.TransportTicketResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.TransportTicketMapper;
import com.example.java_service.Mappers.UserMapper;
import com.example.java_service.Models.TransportTicket;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.TransportTicketRepository;
import com.example.java_service.Repositories.Jpa.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransportTicketService
{
    TransportTicketRepository transportTicketRepository;
    UserRepository userRepository;
    TransportTicketMapper transportTicketMapper;
    UserMapper userMapper;

    private TransportTicketResponse mapToResponse(TransportTicket ticket)
    {
        TransportTicketResponse response = transportTicketMapper.toTransportTicketResponse(ticket);
        if (ticket.getUsers() != null) {
            response.setOwnerTransportTicketResponse(userMapper.toOwnerTransportTicketResponse(ticket.getUsers()));
        }
        return response;
    }

    private void validateTicketType(String type)
    {
        if (type == null) return;
        boolean isValid = type.equals(PredefinedTypeTransportTicket.COACH_TICKET) ||
                type.equals(PredefinedTypeTransportTicket.PLANE_TICKET) ||
                type.equals(PredefinedTypeTransportTicket.TRAIN_TICKET) ||
                type.equals(PredefinedTypeTransportTicket.PASSENGER_SHIP_TICKET);
        if (!isValid) {
            throw new ApiException(ErrorCode.INVALID_TICKET_TYPE);
        }
    }

    private void validateTimeAndPrice(LocalDateTime departureTime, LocalDateTime arrivalTime, BigDecimal price)
    {
        if (departureTime != null && arrivalTime != null) {
            if (!arrivalTime.isAfter(departureTime)) {
                throw new ApiException(ErrorCode.INVALID_TICKET_TIME);
            }
        }
        if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorCode.INVALID_TICKET_PRICE);
        }
    }

    public List<TransportTicketResponse> getAllTickets() {
        return transportTicketRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<TransportTicketResponse> getListTicketApproved() {
        return transportTicketRepository.findByApprovedTrue().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<TransportTicketResponse> getListTicketNotApproved() {
        return transportTicketRepository.findByApprovedFalse().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<TransportTicketResponse> getListTicketCreatedByUser(UUID idUser) {
        return transportTicketRepository.findByUsers_IdUser(idUser).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public ResponseEntity<String> saveTransportTicket(TransportTicketCreationRequest request)
    {
        Users user = userRepository.findById(request.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        // Validate đầu vào
        validateTicketType(request.getTicketType());
        validateTimeAndPrice(request.getDepartureTime(), request.getArrivalTime(), request.getPrice());

        TransportTicket ticket = transportTicketMapper.toTransportTicket(request);
        ticket.setUsers(user);
        ticket.setApproved(false);
        ticket.setStatus(PredefinedStatusTransportTicket.STATUS_TRANSPORT_TICKET_AVAILABLE);

        transportTicketRepository.save(ticket);
        return ResponseEntity.ok("Tạo vé di chuyển thành công!");
    }

    @Transactional
    public ResponseEntity<String> updateTransportTicket(UUID idTicket, TransportTicketUpdateRequest request)
    {
        TransportTicket ticket = transportTicketRepository.findById(idTicket).orElseThrow(
                () -> new ApiException(ErrorCode.TRANSPORT_TICKET_NOT_FOUND)
        );

        validateTicketType(request.getTicketType());

        // Validate status
        if (request.getStatus() != null) {
            boolean isValidStatus = request.getStatus().equals(PredefinedStatusTransportTicket.STATUS_TRANSPORT_TICKET_AVAILABLE) ||
                    request.getStatus().equals(PredefinedStatusTransportTicket.STATUS_TRANSPORT_TICKET_SOLD_OUT);
            if (!isValidStatus) {
                throw new ApiException(ErrorCode.INVALID_STATUS_TICKET);
            }
        }
        LocalDateTime checkDeparture = request.getDepartureTime() != null ? request.getDepartureTime() : ticket.getDepartureTime();
        LocalDateTime checkArrival = request.getArrivalTime() != null ? request.getArrivalTime() : ticket.getArrivalTime();
        validateTimeAndPrice(checkDeparture, checkArrival, request.getPrice());

        transportTicketMapper.updateTransportTicketFromRequest(request, ticket);
        transportTicketRepository.save(ticket);

        return ResponseEntity.ok("Cập nhật vé di chuyển thành công!");
    }

    @Transactional
    public ResponseEntity<String> deleteTransportTicket(UUID idTicket)
    {
        TransportTicket ticket = transportTicketRepository.findById(idTicket).orElseThrow(
                () -> new ApiException(ErrorCode.TRANSPORT_TICKET_NOT_FOUND)
        );

        transportTicketRepository.delete(ticket);
        return ResponseEntity.ok("Xóa vé di chuyển thành công!");
    }

    @Transactional
    public ResponseEntity<String> approveTransportTicket(UUID idTicket, TransportTicketApproveRequest request)
    {
        TransportTicket ticket = transportTicketRepository.findById(idTicket).orElseThrow(
                () -> new ApiException(ErrorCode.TRANSPORT_TICKET_NOT_FOUND)
        );

        ticket.setApproved(request.getApproved());
        transportTicketRepository.save(ticket);

        String message = request.getApproved() ? "Phê duyệt vé thành công!" : "Đã hủy phê duyệt vé!";
        return ResponseEntity.ok(message);
    }
}