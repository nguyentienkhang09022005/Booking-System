package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedItemType;
import com.example.java_service.Dto.Request.CreateItineraryItemRequest;
import com.example.java_service.Dto.Request.CreateItineraryRequest;
import com.example.java_service.Dto.Request.ReorderItemsRequest;
import com.example.java_service.Dto.Request.UpdateItineraryItemRequest;
import com.example.java_service.Dto.Request.UpdateItineraryRequest;
import com.example.java_service.Dto.Response.ItineraryItemResponse;
import com.example.java_service.Dto.Response.ItineraryResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.ItineraryItemMapper;
import com.example.java_service.Mappers.ItineraryMapper;
import com.example.java_service.Models.Itinerary;
import com.example.java_service.Models.ItineraryItem;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.ItineraryItemRepository;
import com.example.java_service.Repositories.Jpa.ItineraryRepository;
import com.example.java_service.Repositories.Jpa.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ItineraryService {

    ItineraryRepository itineraryRepository;
    ItineraryItemRepository itineraryItemRepository;
    UserRepository userRepository;
    ItineraryMapper itineraryMapper;
    ItineraryItemMapper itineraryItemMapper;

    @PersistenceContext
    EntityManager entityManager;

    private static final Map<String, String> ITEM_TYPE_ALIASES = new HashMap<>();
    static {
        ITEM_TYPE_ALIASES.put("car", PredefinedItemType.TRANSPORT);
        ITEM_TYPE_ALIASES.put("taxi", PredefinedItemType.TRANSPORT);
        ITEM_TYPE_ALIASES.put("bus", PredefinedItemType.TRANSPORT);
        ITEM_TYPE_ALIASES.put("train", PredefinedItemType.TRANSPORT);
        ITEM_TYPE_ALIASES.put("airplane", PredefinedItemType.FLIGHT);
        ITEM_TYPE_ALIASES.put("plane", PredefinedItemType.FLIGHT);
        ITEM_TYPE_ALIASES.put("flight", PredefinedItemType.FLIGHT);
        ITEM_TYPE_ALIASES.put("hotel", PredefinedItemType.ACCOMMODATION);
        ITEM_TYPE_ALIASES.put("homestay", PredefinedItemType.ACCOMMODATION);
        ITEM_TYPE_ALIASES.put("resort", PredefinedItemType.ACCOMMODATION);
        ITEM_TYPE_ALIASES.put("restaurant", PredefinedItemType.MEAL);
        ITEM_TYPE_ALIASES.put("food", PredefinedItemType.MEAL);
        ITEM_TYPE_ALIASES.put("sightseeing", PredefinedItemType.ACTIVITY);
        ITEM_TYPE_ALIASES.put("visit", PredefinedItemType.ACTIVITY);
        ITEM_TYPE_ALIASES.put("tour", PredefinedItemType.TOUR);
    }

    @Transactional
    public ItineraryResponse createItinerary(CreateItineraryRequest request) {
        UUID userId = UUID.fromString(request.getIdUser());
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (request.getEndDate() != null && request.getStartDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new ApiException(ErrorCode.INVALID_ITINERARY_DATE);
        }

        Itinerary itinerary = itineraryMapper.toItinerary(request);
        itinerary.setUsers(user);
        Itinerary saved = itineraryRepository.save(itinerary);
        return toResponseWithItems(saved);
    }

    public ItineraryResponse getItinerary(String idItinerary) {
        UUID uuid = UUID.fromString(idItinerary);
        Itinerary itinerary = itineraryRepository.findById(uuid)
                .orElseThrow(() -> new ApiException(ErrorCode.ITINERARY_NOT_FOUND));
        return toResponseWithItems(itinerary);
    }

    public List<ItineraryResponse> getUserItineraries(String idUser) {
        UUID userId = UUID.fromString(idUser);
        List<Itinerary> itineraries = itineraryRepository.findByUsers_IdUserOrderByCreatedAtDesc(userId);
        return itineraries.stream()
                .map(this::toResponseWithItems)
                .collect(Collectors.toList());
    }

    @Transactional
    public ItineraryResponse updateItinerary(String idItinerary, UpdateItineraryRequest request) {
        UUID uuid = UUID.fromString(idItinerary);
        Itinerary itinerary = itineraryRepository.findById(uuid)
                .orElseThrow(() -> new ApiException(ErrorCode.ITINERARY_NOT_FOUND));

        if (request.getEndDate() != null && request.getStartDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new ApiException(ErrorCode.INVALID_ITINERARY_DATE);
        }

        itineraryMapper.updateFromRequest(request, itinerary);
        Itinerary saved = itineraryRepository.save(itinerary);
        return toResponseWithItems(saved);
    }

    @Transactional
    public void deleteItinerary(String idItinerary) {
        UUID uuid = UUID.fromString(idItinerary);
        Itinerary itinerary = itineraryRepository.findById(uuid)
                .orElseThrow(() -> new ApiException(ErrorCode.ITINERARY_NOT_FOUND));
        itineraryItemRepository.deleteByItinerary_IdItinerary(uuid);
        itineraryRepository.delete(itinerary);
    }

    public List<ItineraryItemResponse> getItineraryItems(String idItinerary) {
        UUID uuid = UUID.fromString(idItinerary);
        if (!itineraryRepository.existsById(uuid)) {
            throw new ApiException(ErrorCode.ITINERARY_NOT_FOUND);
        }
        List<ItineraryItem> items = itineraryItemRepository.findByItinerary_IdItineraryOrderByDisplayOrderAsc(uuid);
        return items.stream()
                .map(itineraryItemMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ItineraryItemResponse createItineraryItem(String idItinerary, CreateItineraryItemRequest request) {
        UUID itineraryUuid = UUID.fromString(idItinerary);
        Itinerary itinerary = itineraryRepository.findById(itineraryUuid)
                .orElseThrow(() -> new ApiException(ErrorCode.ITINERARY_NOT_FOUND));

        validateItemType(request.getItemType());
        validateItemDateWithinItinerary(request.getScheduledDate(), itinerary);

        Integer displayOrder = request.getDisplayOrder();
        if (displayOrder == null) {
            Integer maxOrder = itineraryItemRepository.findMaxDisplayOrderByItineraryId(itineraryUuid);
            displayOrder = (maxOrder != null ? maxOrder : 0) + 1;
        } else if (displayOrder < 1) {
            throw new ApiException(ErrorCode.INVALID_DISPLAY_ORDER);
        }

        ItineraryItem item = itineraryItemMapper.toEntity(request, itinerary);
        item.setDisplayOrder(displayOrder);

        ItineraryItem saved = itineraryItemRepository.save(item);
        return itineraryItemMapper.toResponse(saved);
    }

    public ItineraryItemResponse getItineraryItem(String idItem) {
        UUID uuid = UUID.fromString(idItem);
        ItineraryItem item = itineraryItemRepository.findById(uuid)
                .orElseThrow(() -> new ApiException(ErrorCode.ITINERARY_ITEM_NOT_FOUND));
        return itineraryItemMapper.toResponse(item);
    }

    @Transactional
    public ItineraryItemResponse updateItineraryItem(String idItem, UpdateItineraryItemRequest request) {
        UUID uuid = UUID.fromString(idItem);
        ItineraryItem item = itineraryItemRepository.findById(uuid)
                .orElseThrow(() -> new ApiException(ErrorCode.ITINERARY_ITEM_NOT_FOUND));

        if (request.getItemType() != null) {
            validateItemType(request.getItemType());
            item.setItemType(normalizeItemType(request.getItemType()));
        }

        if (request.getScheduledDate() != null) {
            validateItemDateWithinItinerary(request.getScheduledDate(), item.getItinerary());
            item.setScheduledDate(request.getScheduledDate());
        }

        if (request.getDisplayOrder() != null) {
            if (request.getDisplayOrder() < 1) {
                throw new ApiException(ErrorCode.INVALID_DISPLAY_ORDER);
            }
            item.setDisplayOrder(request.getDisplayOrder());
        }

        if (request.getStartTime() != null) {
            item.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            item.setEndTime(request.getEndTime());
        }
        if (request.getSuggestedName() != null) {
            item.setSuggestedName(request.getSuggestedName());
        }
        if (request.getSuggestedLocation() != null) {
            item.setSuggestedLocation(request.getSuggestedLocation());
        }
        if (request.getSuggestedPrice() != null) {
            item.setSuggestedPrice(request.getSuggestedPrice());
        }
        if (request.getEstimatedDurationMinutes() != null) {
            item.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        }
        if (request.getRefId() != null) {
            item.setRefId(UUID.fromString(request.getRefId()));
        }

        ItineraryItem saved = itineraryItemRepository.save(item);
        return itineraryItemMapper.toResponse(saved);
    }

    @Transactional
    public void deleteItineraryItem(String idItem) {
        UUID uuid = UUID.fromString(idItem);
        ItineraryItem item = itineraryItemRepository.findById(uuid)
                .orElseThrow(() -> new ApiException(ErrorCode.ITINERARY_ITEM_NOT_FOUND));

        int deletedOrder = item.getDisplayOrder();
        UUID itineraryId = item.getItinerary().getIdItinerary();

        itineraryItemRepository.delete(item);
        itineraryItemRepository.decrementDisplayOrderForItemsAfter(itineraryId, deletedOrder);
    }

    @Transactional
    public void reorderItems(String idItinerary, ReorderItemsRequest request) {
        UUID itineraryUuid = UUID.fromString(idItinerary);

        if (!itineraryRepository.existsById(itineraryUuid)) {
            throw new ApiException(ErrorCode.ITINERARY_NOT_FOUND);
        }

        List<String> orderedItemIds = request.getOrderedItemIds();
        if (orderedItemIds == null || orderedItemIds.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_DISPLAY_ORDER);
        }

        List<ItineraryItem> existingItems = itineraryItemRepository
                .findByItinerary_IdItineraryOrderByDisplayOrderAsc(itineraryUuid);

        if (existingItems.isEmpty()) {
            throw new ApiException(ErrorCode.ITINERARY_ITEM_NOT_FOUND);
        }

        List<UUID> existingItemIds = existingItems.stream()
                .map(ItineraryItem::getIdItem)
                .collect(Collectors.toList());

        List<UUID> requestedItemIds = orderedItemIds.stream()
                .map(UUID::fromString)
                .collect(Collectors.toList());

        if (requestedItemIds.size() != existingItemIds.size()) {
            throw new ApiException(ErrorCode.INVALID_DISPLAY_ORDER);
        }

        if (!requestedItemIds.containsAll(existingItemIds)) {
            throw new ApiException(ErrorCode.ITINERARY_ITEM_NOT_FOUND);
        }

        StringBuilder caseWhen = new StringBuilder("UPDATE itinerary_item SET display_order = CASE ");
        Map<String, Object> params = new HashMap<>();
        params.put("itineraryId", itineraryUuid);

        for (int i = 0; i < orderedItemIds.size(); i++) {
            String idItem = orderedItemIds.get(i);
            int newOrder = i + 1;
            caseWhen.append("WHEN id_item = CAST(:id").append(i).append(" AS uuid) THEN :order").append(i).append(" ");
            params.put("id" + i, UUID.fromString(idItem));
            params.put("order" + i, newOrder);
        }
        caseWhen.append("END WHERE id_itinerary = :itineraryId");

        jakarta.persistence.Query query = entityManager.createNativeQuery(caseWhen.toString());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        query.executeUpdate();
    }

    @Transactional
    public ItineraryItemResponse updateItemStatus(String idItem, UpdateItineraryItemRequest request) {
        UUID uuid = UUID.fromString(idItem);
        ItineraryItem item = itineraryItemRepository.findById(uuid)
                .orElseThrow(() -> new ApiException(ErrorCode.ITINERARY_ITEM_NOT_FOUND));

        ItineraryItem saved = itineraryItemRepository.save(item);
        return itineraryItemMapper.toResponse(saved);
    }

    @Transactional
    public void confirmItinerary(String idItinerary) {
        UUID uuid = UUID.fromString(idItinerary);
        Itinerary itinerary = itineraryRepository.findById(uuid)
                .orElseThrow(() -> new ApiException(ErrorCode.ITINERARY_NOT_FOUND));
        itinerary.setStatus("CONFIRMED");
        itineraryRepository.save(itinerary);
    }

    @Transactional
    public void updateItineraryStatus(String idItinerary, String status) {
        UUID uuid = UUID.fromString(idItinerary);
        Itinerary itinerary = itineraryRepository.findById(uuid)
                .orElseThrow(() -> new ApiException(ErrorCode.ITINERARY_NOT_FOUND));
        itinerary.setStatus(status);
        itineraryRepository.save(itinerary);
    }

    @Transactional
    public void updateItineraryTotalCost(String idItinerary, BigDecimal totalCost) {
        UUID uuid = UUID.fromString(idItinerary);
        Itinerary itinerary = itineraryRepository.findById(uuid)
                .orElseThrow(() -> new ApiException(ErrorCode.ITINERARY_NOT_FOUND));
        itinerary.setTotalEstimatedCost(totalCost);
        itineraryRepository.save(itinerary);
    }

    public Itinerary getItineraryEntity(String idItinerary) {
        UUID uuid = UUID.fromString(idItinerary);
        return itineraryRepository.findById(uuid)
                .orElseThrow(() -> new ApiException(ErrorCode.ITINERARY_NOT_FOUND));
    }

    @Transactional
    public void saveItineraryItem(ItineraryItem item) {
        itineraryItemRepository.save(item);
    }

    @Transactional
    public void deleteItineraryItemsByItineraryId(UUID itineraryId) {
        itineraryItemRepository.deleteByItinerary_IdItinerary(itineraryId);
    }

    @Transactional
    public void replaceItineraryItems(String idItinerary, List<ItineraryItem> items, BigDecimal totalCost, String status) {
        UUID uuid = UUID.fromString(idItinerary);
        Itinerary itinerary = itineraryRepository.findById(uuid)
                .orElseThrow(() -> new ApiException(ErrorCode.ITINERARY_NOT_FOUND));
        itineraryItemRepository.deleteByItinerary_IdItinerary(uuid);
        if (items != null && !items.isEmpty()) {
            items.forEach(it -> it.setItinerary(itinerary));
            itineraryItemRepository.saveAll(items);
        }
        itinerary.setTotalEstimatedCost(totalCost);
        itinerary.setStatus(status);
        itineraryRepository.save(itinerary);
    }

    public String normalizeItemType(String type) {
        if (type == null || type.isEmpty()) {
            return PredefinedItemType.ACTIVITY;
        }
        String normalized = type.toLowerCase().trim();
        String mapped = ITEM_TYPE_ALIASES.get(normalized);
        if (mapped != null) {
            return mapped;
        }
        if (Arrays.asList(
                PredefinedItemType.FLIGHT,
                PredefinedItemType.ACCOMMODATION,
                PredefinedItemType.TOUR,
                PredefinedItemType.TRANSPORT,
                PredefinedItemType.ACTIVITY,
                PredefinedItemType.MEAL
        ).contains(normalized)) {
            return normalized;
        }
        return PredefinedItemType.ACTIVITY;
    }

    private void validateItemType(String type) {
        if (type == null || type.isEmpty()) {
            return;
        }
        String normalized = type.toLowerCase().trim();
        if (!Arrays.asList(
                PredefinedItemType.FLIGHT,
                PredefinedItemType.ACCOMMODATION,
                PredefinedItemType.TOUR,
                PredefinedItemType.TRANSPORT,
                PredefinedItemType.ACTIVITY,
                PredefinedItemType.MEAL
        ).contains(normalized) && !ITEM_TYPE_ALIASES.containsKey(normalized)) {
            throw new ApiException(ErrorCode.INVALID_ITEM_TYPE);
        }
    }

    private void validateItemDateWithinItinerary(LocalDate itemDate, Itinerary itinerary) {
        if (itemDate == null) {
            return;
        }
        if (itinerary.getStartDate() != null && itemDate.isBefore(itinerary.getStartDate())) {
            throw new ApiException(ErrorCode.INVALID_ITEM_DATE);
        }
        if (itinerary.getEndDate() != null && itemDate.isAfter(itinerary.getEndDate())) {
            throw new ApiException(ErrorCode.INVALID_ITEM_DATE);
        }
    }

    private ItineraryResponse toResponseWithItems(Itinerary itinerary) {
        ItineraryResponse response = itineraryMapper.toResponse(itinerary);
        List<ItineraryItem> items = itineraryItemRepository
                .findByItinerary_IdItineraryOrderByDisplayOrderAsc(itinerary.getIdItinerary());
        response.setItems(items.stream()
                .map(itineraryItemMapper::toResponse)
                .collect(Collectors.toList()));
        return response;
    }
}
