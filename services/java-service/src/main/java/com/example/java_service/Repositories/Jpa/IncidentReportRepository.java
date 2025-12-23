package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentReportRepository extends JpaRepository<IncidentReport, UUID>
{
    List<IncidentReport> findByBooking_Hotel_IdHotel(UUID idHotel);

    List<IncidentReport> findByUser_IdUserAndBooking_IdBooking(UUID idUser, UUID idBooking);
}
