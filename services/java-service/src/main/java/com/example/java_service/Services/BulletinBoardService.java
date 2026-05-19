package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedRole;
import com.example.java_service.Constant.PredefinedStatusBulletin;
import com.example.java_service.Dto.Request.BulletinBoardApprovalRequest;
import com.example.java_service.Dto.Request.BulletinBoardCreationRequest;
import com.example.java_service.Dto.Request.BulletinBoardUpdateRequest;
import com.example.java_service.Dto.Response.BulletinBoardListResponse;
import com.example.java_service.Dto.Response.BulletinBoardResponse;
import com.example.java_service.Dto.Response.NotificationCreatedEvent;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.BulletinBoardMapper;
import com.example.java_service.Models.BulletinBoard;
import com.example.java_service.Models.Images;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.BulletinBoardRepository;
import com.example.java_service.Repositories.Jpa.UserRepository;
import com.example.java_service.Repositories.Mongodb.ImagesRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BulletinBoardService {

    BulletinBoardRepository bulletinBoardRepository;
    BulletinBoardMapper bulletinBoardMapper;
    UserRepository userRepository;
    ImagesRepository imagesRepository;
    ApplicationEventPublisher eventPublisher;

    // Tạo bản tin mới
    @Transactional
    public BulletinBoardResponse createBulletin(BulletinBoardCreationRequest request) {
        // Validate required fields
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new ApiException(ErrorCode.BULLETIN_TITLE_REQUIRED);
        }

        // Validate idUser
        if (request.getIdUser() == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND, "ID người dùng không được để trống!");
        }

        // Validate target audience value
        if (request.getTargetAudience() != null &&
            !List.of(
                PredefinedStatusBulletin.TARGET_AUDIENCE_ALL,
                PredefinedStatusBulletin.TARGET_AUDIENCE_CUSTOMER,
                PredefinedStatusBulletin.TARGET_AUDIENCE_BUSINESS
            ).contains(request.getTargetAudience())) {
            throw new ApiException(ErrorCode.BULLETIN_INVALID_TARGET);
        }

        Users user = userRepository.findById(request.getIdUser())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // Validate target audience - BUSINESS only allowed for ADMIN
        if (!PredefinedRole.ADMIN_ROLE.equals(user.getRole().getRoleName())) {
            if (PredefinedStatusBulletin.TARGET_AUDIENCE_ALL.equals(request.getTargetAudience()) ||
                PredefinedStatusBulletin.TARGET_AUDIENCE_BUSINESS.equals(request.getTargetAudience())) {
                throw new ApiException(ErrorCode.BULLETIN_INVALID_TARGET);
            }
        }

        // Business tạo bản tin -> mặc định CUSTOMER, PENDING, isFeatured = false
        boolean isAdmin = PredefinedRole.ADMIN_ROLE.equals(user.getRole().getRoleName());
        String targetAudience = isAdmin ? request.getTargetAudience() : PredefinedStatusBulletin.TARGET_AUDIENCE_CUSTOMER;

        BulletinBoard bulletin = bulletinBoardMapper.toEntity(request);
        bulletin.setUsers(user);
        bulletin.setTargetAudience(targetAudience);
        bulletin.setIsApproved(isAdmin);
        // Chỉ admin mới được set isFeatured, business tạo luôn = false
        bulletin.setIsFeatured(isAdmin && Boolean.TRUE.equals(request.getIsFeatured()));

        if (isAdmin) {
            bulletin.setStatus(PredefinedStatusBulletin.STATUS_BULLETIN_APPROVED);
            bulletin.setApprovedAt(LocalDateTime.now());
        } else {
            bulletin.setStatus(PredefinedStatusBulletin.STATUS_BULLETIN_PENDING);
        }

        BulletinBoard saved = bulletinBoardRepository.save(bulletin);

        // Lưu image vào MongoDB nếu có
        saveBulletinImage(saved.getIdBulletin(), request.getImageUrl());

        return buildResponse(saved);
    }

    // Lấy danh sách bản tin active theo audience (cũ - giữ lại để tương thích)
    public List<BulletinBoardResponse> getActiveBulletins(String targetAudience) {
        List<BulletinBoard> bulletins = bulletinBoardRepository.findActiveByAudience(
                targetAudience, LocalDateTime.now());

        if (bulletins.isEmpty()) {
            return List.of();
        }

        return buildResponseList(bulletins);
    }

    // Lấy danh sách bản tin theo 5 loại cho Customer
    public BulletinBoardListResponse getBulletinListByCategories(String targetAudience) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiringThreshold = now.plusDays(7); // 7 ngày tới

        // 1. Tin nổi bật (leaderboard) - lấy top 1
        List<BulletinBoard> leaders = bulletinBoardRepository.findFeaturedLeaders(targetAudience, now);
        List<BulletinBoardResponse> leaderboard = buildResponseList(leaders);

        // Lấy id của leaderboard để exclude
        List<UUID> leaderIds = leaderboard.stream().map(BulletinBoardResponse::getIdBulletin).toList();

        // 2. Tin nổi bật khác (grid 4 cột) - exclude leaderboard
        List<BulletinBoard> featuredList = leaderIds.isEmpty()
                ? List.of()
                : bulletinBoardRepository.findFeaturedOthers(targetAudience, now, leaderIds);
        List<BulletinBoardResponse> featuredGrid = buildResponseList(featuredList);

        // Lấy id để exclude
        List<UUID> featuredIds = featuredGrid.stream().map(BulletinBoardResponse::getIdBulletin).toList();
        List<UUID> excludeIds = new java.util.ArrayList<>(leaderIds);
        excludeIds.addAll(featuredIds);

        // 3. Tin mới (horizontal scroll)
        List<BulletinBoard> newestList = bulletinBoardRepository.findNewest(targetAudience, now);
        // Exclude featured
        newestList = newestList.stream()
                .filter(b -> !excludeIds.contains(b.getIdBulletin()))
                .toList();
        List<BulletinBoardResponse> newestScroll = buildResponseList(newestList);

        // 4. Tin sắp hết hạn
        List<BulletinBoard> expiringList = bulletinBoardRepository.findExpiringSoon(targetAudience, now, expiringThreshold);
        List<BulletinBoardResponse> expiringSoon = buildResponseList(expiringList);

        // 5. Tin thường (grid 3 cột)
        List<UUID> allExcludeIds = new java.util.ArrayList<>(excludeIds);
        allExcludeIds.addAll(expiringSoon.stream().map(BulletinBoardResponse::getIdBulletin).toList());
        List<BulletinBoard> regularList = allExcludeIds.isEmpty()
                ? List.of()
                : bulletinBoardRepository.findRegular(targetAudience, now, allExcludeIds);
        List<BulletinBoardResponse> regularGrid = buildResponseList(regularList);

        return BulletinBoardListResponse.builder()
                .leaderboard(leaderboard)
                .featuredGrid(featuredGrid)
                .newestScroll(newestScroll)
                .expiringSoon(expiringSoon)
                .regularGrid(regularGrid)
                .build();
    }

    // Lấy chi tiết bản tin
    public BulletinBoardResponse getBulletinDetail(UUID bulletinId) {
        BulletinBoard bulletin = bulletinBoardRepository.findById(bulletinId)
                .orElseThrow(() -> new ApiException(ErrorCode.BULLETIN_NOT_FOUND));
        return buildResponse(bulletin);
    }

    // Cập nhật bản tin
    @Transactional
    public BulletinBoardResponse updateBulletin(UUID bulletinId, BulletinBoardUpdateRequest request, UUID userId) {
        BulletinBoard bulletin = bulletinBoardRepository.findById(bulletinId)
                .orElseThrow(() -> new ApiException(ErrorCode.BULLETIN_NOT_FOUND));

        // Kiểm tra quyền: author hoặc admin mới được sửa
        boolean isAuthor = bulletin.getUsers().getIdUser().equals(userId);
        boolean isAdmin = false;
        if (!isAuthor) {
            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
            isAdmin = PredefinedRole.ADMIN_ROLE.equals(user.getRole().getRoleName());
        }

        if (!isAuthor && !isAdmin) {
            throw new ApiException(ErrorCode.BULLETIN_ACCESS_DENIED);
        }

        // Không cho sửa bản tin đã duyệt (chỉ admin được sửa bản tin đã duyệt)
        if (PredefinedStatusBulletin.STATUS_BULLETIN_APPROVED.equals(bulletin.getStatus()) && !isAdmin) {
            throw new ApiException(ErrorCode.BULLETIN_ALREADY_APPROVED, "Không thể cập nhật bản tin đã được phê duyệt!");
        }

        // Validate title nếu có
        if (request.getTitle() != null && request.getTitle().trim().isEmpty()) {
            throw new ApiException(ErrorCode.BULLETIN_TITLE_REQUIRED);
        }

        bulletinBoardMapper.updateEntityFromRequest(request, bulletin);
        BulletinBoard saved = bulletinBoardRepository.save(bulletin);

        // Cập nhật image nếu có
        updateBulletinImage(saved.getIdBulletin(), request.getImageUrl());

        return buildResponse(saved);
    }

    // Xóa bản tin
    @Transactional
    public void deleteBulletin(UUID bulletinId, UUID userId) {
        BulletinBoard bulletin = bulletinBoardRepository.findById(bulletinId)
                .orElseThrow(() -> new ApiException(ErrorCode.BULLETIN_NOT_FOUND));

        // Kiểm tra quyền: author hoặc admin mới được xóa
        boolean isAuthor = bulletin.getUsers().getIdUser().equals(userId);
        boolean isAdmin = false;
        if (!isAuthor) {
            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
            isAdmin = PredefinedRole.ADMIN_ROLE.equals(user.getRole().getRoleName());
        }

        if (!isAuthor && !isAdmin) {
            throw new ApiException(ErrorCode.BULLETIN_ACCESS_DENIED);
        }

        bulletinBoardRepository.delete(bulletin);
        imagesRepository.deleteByRefId(bulletinId.toString());
    }

    // Lấy danh sách bản tin chờ duyệt (Admin)
    public Page<BulletinBoardResponse> getPendingBulletins(Pageable pageable) {
        Page<BulletinBoard> bulletins = bulletinBoardRepository.findByStatus(
                PredefinedStatusBulletin.STATUS_BULLETIN_PENDING, pageable);
        return bulletins.map(this::buildResponse);
    }

    // Lấy danh sách bản tin theo user (admin hoặc business xem bản tin đã tạo)
    public List<BulletinBoardResponse> getMyBulletins(UUID userId, String status) {
        List<BulletinBoard> bulletins;
        if (status != null && !status.trim().isEmpty()) {
            bulletins = bulletinBoardRepository.findByUserIdAndStatus(userId, status);
        } else {
            bulletins = bulletinBoardRepository.findByUserId(userId);
        }
        return buildResponseList(bulletins);
    }

    // Admin duyệt bản tin
    @Transactional
    public BulletinBoardResponse approveBulletin(UUID bulletinId) {
        BulletinBoard bulletin = bulletinBoardRepository.findById(bulletinId)
                .orElseThrow(() -> new ApiException(ErrorCode.BULLETIN_NOT_FOUND));

        if (PredefinedStatusBulletin.STATUS_BULLETIN_APPROVED.equals(bulletin.getStatus())) {
            throw new ApiException(ErrorCode.BULLETIN_ALREADY_APPROVED);
        }

        bulletin.setStatus(PredefinedStatusBulletin.STATUS_BULLETIN_APPROVED);
        bulletin.setIsApproved(true);
        bulletin.setApprovedAt(LocalDateTime.now());

        BulletinBoard saved = bulletinBoardRepository.save(bulletin);

        // Gửi notification SYSTEM cho business owner khi duyệt
        sendApprovalNotification(bulletin);

        return buildResponse(saved);
    }

    // Admin từ chối bản tin
    @Transactional
    public BulletinBoardResponse rejectBulletin(UUID bulletinId, BulletinBoardApprovalRequest request) {
        BulletinBoard bulletin = bulletinBoardRepository.findById(bulletinId)
                .orElseThrow(() -> new ApiException(ErrorCode.BULLETIN_NOT_FOUND));

        if (PredefinedStatusBulletin.STATUS_BULLETIN_REJECTED.equals(bulletin.getStatus())) {
            throw new ApiException(ErrorCode.BULLETIN_ALREADY_REJECTED);
        }

        bulletin.setStatus(PredefinedStatusBulletin.STATUS_BULLETIN_REJECTED);
        bulletin.setIsApproved(false);

        BulletinBoard saved = bulletinBoardRepository.save(bulletin);

        // Gửi notification SYSTEM cho business owner khi từ chối
        sendRejectionNotification(bulletin, request.getReason());

        return buildResponse(saved);
    }

    // ===== Helper methods =====

    private BulletinBoardResponse buildResponse(BulletinBoard bulletin) {
        BulletinBoardResponse response = bulletinBoardMapper.toResponse(bulletin);

        // Set author info
        Users author = bulletin.getUsers();
        response.setAuthor(BulletinBoardResponse.AuthorInfo.builder()
                .idUser(author.getIdUser())
                .firstName(author.getFirstName())
                .lastName(author.getLastName())
                .email(author.getEmail())
                .build());

        // Set images từ MongoDB
        List<Images> images = imagesRepository.findByRefId(bulletin.getIdBulletin().toString());
        response.setImages(images.stream()
                .map(Images::getUrl)
                .collect(Collectors.toList()));

        return response;
    }

    private List<BulletinBoardResponse> buildResponseList(List<BulletinBoard> bulletins) {
        if (bulletins.isEmpty()) {
            return List.of();
        }

        // Bulk fetch images để tránh N+1
        List<String> bulletinIds = bulletins.stream()
                .map(b -> b.getIdBulletin().toString())
                .collect(Collectors.toList());

        List<Images> allImages = imagesRepository.findByRefIdIn(bulletinIds);
        var imagesByBulletinId = allImages.stream()
                .collect(Collectors.groupingBy(Images::getRefId));

        return bulletins.stream()
                .map(bulletin -> {
                    BulletinBoardResponse response = bulletinBoardMapper.toResponse(bulletin);
                    Users author = bulletin.getUsers();
                    response.setAuthor(BulletinBoardResponse.AuthorInfo.builder()
                            .idUser(author.getIdUser())
                            .firstName(author.getFirstName())
                            .lastName(author.getLastName())
                            .email(author.getEmail())
                            .build());

                    List<String> urls = imagesByBulletinId.getOrDefault(bulletin.getIdBulletin().toString(), List.of())
                            .stream()
                            .map(Images::getUrl)
                            .collect(Collectors.toList());
                    response.setImages(urls);

                    return response;
                }).collect(Collectors.toList());
    }

    // ===== Helper methods cho Image =====

    private void saveBulletinImage(UUID bulletinId, String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) return;

        Images image = new Images();
        image.setUrl(imageUrl);
        image.setRefId(bulletinId.toString());
        image.setType("bulletin");
        imagesRepository.save(image);
    }

    private void updateBulletinImage(UUID bulletinId, String imageUrl) {
        imagesRepository.deleteByRefId(bulletinId.toString());
        saveBulletinImage(bulletinId, imageUrl);
    }

    // ===== Notification methods =====

    private void sendApprovalNotification(BulletinBoard bulletin) {
        try {
            NotificationCreatedEvent event = NotificationCreatedEvent.builder()
                    .idNotification(UUID.randomUUID())
                    .idUser(bulletin.getUsers().getIdUser())
                    .title("Bản tin được phê duyệt")
                    .content(String.format("Bản tin '%s' của bạn đã được phê duyệt và hiển thị trên hệ thống!", bulletin.getTitle()))
                    .type("system")
                    .idRef(bulletin.getIdBulletin().toString())
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            eventPublisher.publishEvent(event);
        } catch (Exception ex) {
            log.error("Failed to send approval notification for bulletin {}", bulletin.getIdBulletin(), ex);
        }
    }

    private void sendRejectionNotification(BulletinBoard bulletin, String reason) {
        try {
            String content = String.format("Bản tin '%s' đã bị từ chối.", bulletin.getTitle());
            if (reason != null && !reason.isEmpty()) {
                content += " Lý do: " + reason;
            }

            NotificationCreatedEvent event = NotificationCreatedEvent.builder()
                    .idNotification(UUID.randomUUID())
                    .idUser(bulletin.getUsers().getIdUser())
                    .title("Bản tin bị từ chối")
                    .content(content)
                    .type("system")
                    .idRef(bulletin.getIdBulletin().toString())
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            eventPublisher.publishEvent(event);
        } catch (Exception ex) {
            log.error("Failed to send rejection notification for bulletin {}", bulletin.getIdBulletin(), ex);
        }
    }
}