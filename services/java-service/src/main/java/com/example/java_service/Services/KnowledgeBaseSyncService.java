package com.example.java_service.Services;

import com.example.java_service.Models.*;
import com.example.java_service.Repositories.Jpa.*;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KnowledgeBaseSyncService
{
    GeneralRegulationRepository regulationRepository;
    TourRepository tourRepository;
    AccommodationRepository accommodationRepository;
    ServiceRepository serviceRepository;
    VoucherRepository voucherRepository;
    TransportTicketRepository transportTicketRepository;
    RoomRepository roomRepository;
    TypeRoomRepository typeRoomRepository;
    TypeServiceRepository typeServiceRepository;
    EmbeddingModel embeddingModel;
    EmbeddingStore<TextSegment> embeddingStore;
    RagCacheService ragCacheService;

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;
    private static final int BATCH_SIZE = 20;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 50_000;

@Transactional(readOnly = true)
    public String syncDataToVectorDb()
    {
        log.info("Bắt đầu đồng bộ dữ liệu vào Vector DB");

        StringBuilder corpus = new StringBuilder();
        int totalSegments = 0;

        totalSegments += syncGeneralRegulations(corpus);
        totalSegments += syncTours(corpus);
        totalSegments += syncAccommodations(corpus);
        totalSegments += syncRooms(corpus);
        totalSegments += syncServices(corpus);
        totalSegments += syncVouchers(corpus);
        totalSegments += syncTransportTickets(corpus);
        totalSegments += syncRoomTypes(corpus);
        totalSegments += syncServiceTypes(corpus);

        if (corpus.length() > 0) {
            log.info("Chunk và embed {} ký tự dữ liệu", corpus.length());
            Document document = Document.from(corpus.toString());
            List<TextSegment> segments = DocumentSplitters.recursive(CHUNK_SIZE, CHUNK_OVERLAP).split(document);

            embedAndStoreInBatches(segments);

            log.info("Đã đồng bộ {} segments vào Vector DB", segments.size());
        }

        ragCacheService.evictAllRagCache();
        log.info("Đã xóa cache RAG sau khi sync hoàn tất");

return String.format("Đồng bộ thành công %d segments!", totalSegments);
    }

    private void embedAndStoreInBatches(List<TextSegment> segments) {
        int totalBatches = (int) Math.ceil((double) segments.size() / BATCH_SIZE);
        log.info("Processing {} segments in {} batches", segments.size(), totalBatches);

        for (int i = 0; i < segments.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, segments.size());
            List<TextSegment> batch = segments.subList(i, end);
            int batchNumber = (i / BATCH_SIZE) + 1;

            int attempts = 0;
            boolean success = false;

            while (attempts < MAX_RETRIES && !success) {
                try {
                    List<Embedding> embeddings = embeddingModel.embedAll(batch).content();
                    embeddingStore.addAll(embeddings, batch);
                    log.info("Processed batch {}/{} ({} segments)",
                            batchNumber, totalBatches, batch.size());
                    success = true;
                } catch (Exception e) {
                    attempts++;
                    if (attempts >= MAX_RETRIES) {
                        log.error("Failed to process batch {} after {} attempts: {}",
                                batchNumber, MAX_RETRIES, e.getMessage());
                        throw e;
                    }

                    boolean isRateLimit = e.getMessage() != null &&
                            (e.getMessage().contains("RESOURCE_EXHAUSTED") ||
                             e.getMessage().contains("429") ||
                             e.getMessage().contains("rate limit"));

                    if (isRateLimit) {
                        log.warn("Rate limit hit on batch {}, waiting {}ms before retry (attempt {}/{})",
                                batchNumber, RETRY_DELAY_MS, attempts, MAX_RETRIES);
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted while waiting to retry", ie);
                        }
                    } else {
                        log.error("Non-rate-limit error on batch {}: {}", batchNumber, e.getMessage());
                        throw e;
                    }
                }
            }
        }
    }

    private int syncGeneralRegulations(StringBuilder corpus)
    {
        List<GeneralRegulation> regulations = regulationRepository.findAll();
        if (regulations.isEmpty()) {
            return 0;
        }

        corpus.append("\n\n===== QUY ĐỊNH VÀ CHÍNH SÁCH KHÁCH SẠN =====\n\n");
        for (GeneralRegulation reg : regulations) {
            String hotelName = reg.getAccommodation() != null ? reg.getAccommodation().getAccommodationName() : "Khách sạn không xác định";
            corpus.append(String.format("Khách sạn: %s\n", hotelName));
            corpus.append(String.format("Quy định: %s\n", reg.getRegulationName()));
            corpus.append(String.format("Nội dung: %s\n\n", reg.getRegulationContent()));
        }

        log.info("Đã xử lý {} quy định", regulations.size());
        return regulations.size();
    }

    private int syncTours(StringBuilder corpus)
    {
        List<Tour> tours = tourRepository.findAllToursWithSchedules();
        if (tours.isEmpty()) {
            return 0;
        }

        corpus.append("\n\n===== THÔNG TIN TOUR DU LỊCH =====\n\n");
        for (Tour tour : tours) {
            corpus.append(String.format("Tour: %s\n", tour.getTourName()));
            corpus.append(String.format("Mô tả: %s\n", tour.getDescription() != null ? tour.getDescription() : "Không có mô tả"));
            corpus.append(String.format("Thời lượng: %d phút\n", tour.getDurationMinutes()));
            corpus.append(String.format("Giá vé người lớn: %s VNĐ\n", tour.getAdultPrice()));
            corpus.append(String.format("Giá vé trẻ em: %s VNĐ\n", tour.getChildPrice()));
            corpus.append(String.format("Điểm tập trung: %s\n", nullSafe(tour.getMeetingPoint())));
            corpus.append(String.format("Điểm trả khách: %s\n", nullSafe(tour.getDropOffPoint())));
            corpus.append(String.format("Ngôn ngữ hỗ trợ: %s\n", nullSafe(tour.getLanguages())));
            corpus.append(String.format("Chính sách hủy: %s\n", nullSafe(tour.getCancellationPolicy())));

            if (tour.getInclusions() != null) corpus.append(String.format("Bao gồm: %s\n", tour.getInclusions()));
            if (tour.getExclusions() != null) corpus.append(String.format("Không bao gồm: %s\n", tour.getExclusions()));
            if (tour.getAdditionalInfo() != null) corpus.append(String.format("Khuyến cáo: %s\n", tour.getAdditionalInfo()));

            corpus.append("\n");
        }

        log.info("Đã xử lý {} tours", tours.size());
        return tours.size();
    }

    private int syncAccommodations(StringBuilder corpus)
    {
        List<Accommodation> accommodations = accommodationRepository.findAllByApprovedTrue();
        if (accommodations.isEmpty()) {
            return 0;
        }

        corpus.append("\n\n===== THÔNG TIN NƠI CƯ TRÚ =====\n\n");
        for (Accommodation acc : accommodations) {
            corpus.append(String.format("Tên: %s\n", acc.getAccommodationName()));
            corpus.append(String.format("Loại: %s\n", nullSafe(acc.getAccommodationType())));
            corpus.append(String.format("Địa chỉ: %s, %s, %s, %s\n",
                    nullSafe(acc.getStreet()),
                    nullSafe(acc.getWard()),
                    nullSafe(acc.getCity()),
                    nullSafe(acc.getProvince())));
            corpus.append(String.format("Mô tả: %s\n", nullSafe(acc.getDescription())));

            if (acc.getCheckinTime() != null) corpus.append(String.format("Giờ nhận phòng: %s\n", acc.getCheckinTime()));
            if (acc.getCheckoutTime() != null) corpus.append(String.format("Giờ trả phòng: %s\n", acc.getCheckoutTime()));

            corpus.append("\n");
        }

        log.info("Đã xử lý {} accommodations", accommodations.size());
        return accommodations.size();
    }

    private int syncRooms(StringBuilder corpus)
    {
        List<Room> rooms = roomRepository.findAll();
        if (rooms.isEmpty()) {
            return 0;
        }

        corpus.append("\n\n===== LOẠI PHÒNG VÀ GIÁ =====\n\n");
        for (Room room : rooms) {
            String hotelName = room.getAccommodation() != null ? room.getAccommodation().getAccommodationName() : "Khách sạn không xác định";
            corpus.append(String.format("Khách sạn: %s\n", hotelName));
            corpus.append(String.format("Loại phòng: %s\n", nullSafe(room.getTypeRoom().getTypeRoom())));
            corpus.append(String.format("Tên phòng: %s\n", room.getRoomName()));
            corpus.append(String.format("Giá: %s VNĐ\n", room.getRoomPrice()));
            corpus.append(String.format("Kích thước: %s\n", nullSafe(room.getSize())));
            corpus.append(String.format("Tầm nhìn: %s\n", nullSafe(room.getView())));
            corpus.append(String.format("Trạng thái: %s\n", room.getStatus()));
            corpus.append(String.format("Số lượng: %d\n", room.getQuantity() != null ? room.getQuantity() : 0));
            corpus.append(String.format("Quy định: %s\n", nullSafe(room.getRegulations())));
            corpus.append("\n");
        }

        log.info("Đã xử lý {} rooms", rooms.size());
        return rooms.size();
    }

    private int syncServices(StringBuilder corpus)
    {
        List<com.example.java_service.Models.Service> services = serviceRepository.findAll();
        if (services.isEmpty()) {
            return 0;
        }

        corpus.append("\n\n===== DỊCH VỤ KHÁCH SẠN =====\n\n");
        for (com.example.java_service.Models.Service svc : services) {
            String hotelName = svc.getAccommodation() != null ? svc.getAccommodation().getAccommodationName() : "Khách sạn không xác định";
            corpus.append(String.format("Khách sạn: %s\n", hotelName));
            corpus.append(String.format("Dịch vụ: %s\n", svc.getServiceName()));
            corpus.append(String.format("Loại: %s\n", nullSafe(svc.getTypeService().getTypeService())));
            corpus.append(String.format("Giá: %s VNĐ\n", svc.getServicePrice()));
            corpus.append(String.format("Trạng thái: %s\n", svc.getStatus()));
            corpus.append(String.format("Mô tả: %s\n\n", nullSafe(svc.getDescription())));
        }

        log.info("Đã xử lý {} services", services.size());
        return services.size();
    }

    private int syncVouchers(StringBuilder corpus)
    {
        List<Voucher> vouchers = voucherRepository.findAll().stream().filter(Voucher::getApproved).toList();
        if (vouchers.isEmpty()) {
            return 0;
        }

        corpus.append("\n\n===== MÃ GIẢM GIÁ VÀ KHUYẾN MÃI =====\n\n");
        for (Voucher v : vouchers) {
            String hotelName = v.getAccommodation() != null ? v.getAccommodation().getAccommodationName() : "Hệ thống";
            corpus.append(String.format("Voucher: %s\n", v.getNameVoucher()));
            corpus.append(String.format("Khách sạn: %s\n", hotelName));
            if (v.getCode() != null) corpus.append(String.format("Mã code: %s\n", v.getCode()));
            corpus.append(String.format("Giảm giá: %s%%\n", v.getDiscountPercent()));
            corpus.append(String.format("Điều kiện: %s\n", nullSafe(v.getConditions())));
            corpus.append(String.format("Áp dụng cho: %s\n", nullSafe(v.getApplyTo())));

            if (v.getStartDate() != null && v.getEndDate() != null) {
                corpus.append(String.format("Thời gian: %s đến %s\n", v.getStartDate(), v.getEndDate()));
            }
            if (v.getMinDiscountAmount() != null || v.getMaxDiscountAmount() != null) {
                corpus.append(String.format("Mức giảm: %s - %s VNĐ\n",
                        v.getMinDiscountAmount() != null ? v.getMinDiscountAmount() : "0",
                        v.getMaxDiscountAmount() != null ? v.getMaxDiscountAmount() : "Không giới hạn"));
            }
            corpus.append("\n");
        }

        log.info("Đã xử lý {} vouchers", vouchers.size());
        return vouchers.size();
    }

    private int syncTransportTickets(StringBuilder corpus)
    {
        List<TransportTicket> tickets = transportTicketRepository.findAll().stream()
                .filter(TransportTicket::getApproved)
                .toList();
        if (tickets.isEmpty()) {
            return 0;
        }

        corpus.append("\n\n===== VÉ PHƯƠNG TIỆN DI CHUYỂN =====\n\n");
        for (TransportTicket ticket : tickets) {
            corpus.append(String.format("Nhà cung cấp: %s\n", nullSafe(ticket.getProviderName())));
            corpus.append(String.format("Loại vé: %s\n", nullSafe(ticket.getTicketType())));
            corpus.append(String.format("Hạng vé: %s\n", nullSafe(ticket.getTicketClass())));
            corpus.append(String.format("Điểm đi: %s\n", nullSafe(ticket.getDepartureLocation())));
            corpus.append(String.format("Điểm đến: %s\n", nullSafe(ticket.getArrivalLocation())));
            corpus.append(String.format("Giá: %s VNĐ\n", ticket.getPrice()));
            corpus.append(String.format("Trạng thái: %s\n", ticket.getStatus()));

            if (ticket.getDepartureTime() != null) {
                corpus.append(String.format("Giờ khởi hành: %s\n", ticket.getDepartureTime()));
            }
            if (ticket.getArrivalTime() != null) {
                corpus.append(String.format("Giờ đến: %s\n", ticket.getArrivalTime()));
            }
            corpus.append("\n");
        }

        log.info("Đã xử lý {} transport tickets", tickets.size());
        return tickets.size();
    }

    private int syncRoomTypes(StringBuilder corpus)
    {
        List<TypeRoom> types = typeRoomRepository.findAll();
        if (types.isEmpty()) {
            return 0;
        }

        corpus.append("\n\n===== DANH SÁCH LOẠI PHÒNG =====\n\n");
        for (TypeRoom type : types) {
            corpus.append(String.format("Loại phòng: %s\n", type.getTypeRoom()));
            corpus.append(String.format("Mô tả: %s\n\n", nullSafe(type.getDescription())));
        }

        log.info("Đã xử lý {} room types", types.size());
        return types.size();
    }

    private int syncServiceTypes(StringBuilder corpus)
    {
        List<TypeService> types = typeServiceRepository.findAll();
        if (types.isEmpty()) {
            return 0;
        }

        corpus.append("\n\n===== DANH SÁCH LOẠI DỊCH VỤ =====\n\n");
        for (TypeService type : types) {
            corpus.append(String.format("Loại dịch vụ: %s\n", type.getTypeService()));
            corpus.append(String.format("Mô tả: %s\n\n", nullSafe(type.getDescription())));
        }

        log.info("Đã xử lý {} service types", types.size());
        return types.size();
    }

    private String nullSafe(String value) {
        return value != null ? value : "Không có thông tin";
    }
}