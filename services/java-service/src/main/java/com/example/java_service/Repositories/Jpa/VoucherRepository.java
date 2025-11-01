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

    List<Voucher> findVoucherByApprovedTrueAndHotel_IdHotel(UUID idHotel);

    List<Voucher> findVoucherByApprovedFalse();

    List<Voucher> findVoucherByHotel_IdHotel(UUID idHotel);

    // Giảm số lượng voucher khi lưu
    @Modifying
    @Query("""
    UPDATE Voucher v
    SET v.quantity = v.quantity - :defaultQuantity
    WHERE v.idVoucher = :idVoucher AND v.quantity >= :defaultQuantity""")
    int decreaseVoucherQuantity(@Param("idVoucher") UUID idVoucher,
                                @Param("defaultQuantity") int defaultQuantity);
}
