package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.ItineraryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, UUID> {
    List<ItineraryItem> findByItinerary_IdItineraryOrderByDisplayOrderAsc(UUID idItinerary);
    Optional<ItineraryItem> findByItinerary_IdItineraryAndRefId(UUID itineraryId, UUID refId);
    void deleteByItinerary_IdItinerary(UUID idItinerary);
    List<ItineraryItem> findByItinerary_IdItineraryAndDisplayOrderGreaterThanOrderByDisplayOrderAsc(UUID idItinerary, Integer displayOrder);

    @Query("SELECT COALESCE(MAX(i.displayOrder), 0) FROM ItineraryItem i WHERE i.itinerary.idItinerary = :itineraryId")
    Integer findMaxDisplayOrderByItineraryId(@Param("itineraryId") UUID itineraryId);

    @Modifying
    @Query("UPDATE ItineraryItem i SET i.displayOrder = i.displayOrder - 1 WHERE i.itinerary.idItinerary = :itineraryId AND i.displayOrder > :deletedOrder")
    void decrementDisplayOrderForItemsAfter(@Param("itineraryId") UUID itineraryId, @Param("deletedOrder") Integer deletedOrder);
}
