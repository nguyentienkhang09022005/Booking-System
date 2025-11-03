package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.UserVoucher;
import com.example.java_service.Models.UserVoucherId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, UserVoucherId>
{
    @Modifying
    @Query("""
           SELECT uv FROM UserVoucher uv
           JOIN FETCH uv.voucher v
           JOIN FETCH uv.user u
           WHERE u.idUser = :idUser AND v.endDate >= :toDay""")
    List<UserVoucher> findValidByUserIdAndVoucherEndDateAfterOrEqual(@Param("idUser") UUID idUser,
                                                                     @Param("toDay") LocalDate toDay);

    @Query("""
    SELECT uv
    FROM UserVoucher uv
    WHERE uv.voucher.idVoucher IN (
        SELECT av.voucher.idVoucher
        FROM AppliedVoucher av
        WHERE av.booking.idBooking = :idBooking
    )
    AND uv.voucher.endDate >= CURRENT_TIMESTAMP
""")
    List<UserVoucher> findUsedVouchersByBookingId(@Param("idBooking") UUID idBooking);
}
