package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, UUID>
{
    Boolean existsVoucherByCode(String code);

    // Lấy voucher đã duyệt: bao gồm voucher của nơi cư trú cụ thể hoặc voucher của hệ thống
    @Query("SELECT v FROM Voucher v WHERE v.approved = true AND (v.accommodation.idAccommodation = :idAccommodation OR UPPER(v.sponsorType) = 'SYSTEM')")
    List<Voucher> findApprovedVouchersForAccommodationAndSystem(@Param("idAccommodation") UUID idAccommodation);

    List<Voucher> findVoucherByApprovedFalse();

    List<Voucher> findVoucherByAccommodation_IdAccommodation(UUID idAccommodation);

    @Modifying
    @Query("""
    UPDATE Voucher v
    SET v.quantity = v.quantity - :defaultQuantity
    WHERE v.idVoucher = :idVoucher AND v.quantity >= :defaultQuantity""")
    int decreaseVoucherQuantity(@Param("idVoucher") UUID idVoucher,
                                @Param("defaultQuantity") int defaultQuantity);
}