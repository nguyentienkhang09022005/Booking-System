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

    // Dành cho Booking Room
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

    @Query("""
    SELECT uv
    FROM UserVoucher uv
    WHERE uv.voucher.idVoucher IN (
        SELECT av.voucher.idVoucher
        FROM AppliedVoucher av
        WHERE av.bookingTour.idBookingTour = :idBookingTour
    )
    AND uv.voucher.endDate >= CURRENT_TIMESTAMP
""")
    List<UserVoucher> findUsedVouchersByBookingTourId(@Param("idBookingTour") UUID idBookingTour);

    @Query("""
    SELECT uv
    FROM UserVoucher uv
    WHERE uv.voucher.idVoucher IN (
        SELECT av.voucher.idVoucher
        FROM AppliedVoucher av
        WHERE av.bookingTransport.idBookingTransport = :idBookingTransport
    )
    AND uv.voucher.endDate >= CURRENT_TIMESTAMP
""")
    List<UserVoucher> findUsedVouchersByBookingTicketId(@Param("idBookingTransport") UUID idBookingTransport);

    @Query("""
    SELECT uv.voucher.idVoucher
    FROM UserVoucher uv
    WHERE uv.user.idUser = :idUser AND uv.voucher.idVoucher IN :idVouchers
""")
    List<UUID> findSavedVoucherIdsByUserAndVoucherIds(@Param("idUser") UUID idUser,
                                                      @Param("idVouchers") List<UUID> idVouchers);
}