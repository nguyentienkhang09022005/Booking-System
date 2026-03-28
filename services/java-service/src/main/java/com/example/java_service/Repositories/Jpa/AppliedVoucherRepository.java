package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.AppliedVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppliedVoucherRepository extends JpaRepository<AppliedVoucher, UUID>
{

    void deleteAppliedVoucherByBooking_IdBooking(UUID idBooking);

    @Query("""
        SELECT av
        FROM AppliedVoucher av
        JOIN FETCH av.voucher v
        WHERE av.booking.idBooking = :idBooking""")
    List<AppliedVoucher> findAppliedVouchersWithVoucherByBookingId(@Param("idBooking") UUID idBooking);

    @Query("""
        SELECT av
        FROM AppliedVoucher av
        JOIN FETCH av.voucher v
        WHERE av.booking.idBooking IN :idBookings""")
    List<AppliedVoucher> findAppliedVouchersWithVoucherByBookingIds(@Param("idBookings") List<UUID> idBookings);


    void deleteAppliedVoucherByBookingTour_IdBookingTour(UUID idBookingTour);

    @Query("""
        SELECT av
        FROM AppliedVoucher av
        JOIN FETCH av.voucher v
        WHERE av.bookingTour.idBookingTour = :idBookingTour""")
    List<AppliedVoucher> findAppliedVouchersWithVoucherByBookingTourId(@Param("idBookingTour") UUID idBookingTour);

    @Query("""
        SELECT av
        FROM AppliedVoucher av
        JOIN FETCH av.voucher v
        WHERE av.bookingTour.idBookingTour IN :idBookingTours""")
    List<AppliedVoucher> findAppliedVouchersWithVoucherByBookingTourIds(@Param("idBookingTours") List<UUID> idBookingTours);

    void deleteAppliedVoucherByBookingTransport_IdBookingTransport(UUID idBookingTransport);

    @Query("""
        SELECT av
        FROM AppliedVoucher av
        JOIN FETCH av.voucher v
        WHERE av.bookingTransport.idBookingTransport = :idBookingTransport""")
    List<AppliedVoucher> findAppliedVouchersWithVoucherByBookingTransportId(@Param("idBookingTransport") UUID idBookingTransport);

    @Query("""
        SELECT av
        FROM AppliedVoucher av
        JOIN FETCH av.voucher v
        WHERE av.bookingTransport.idBookingTransport IN :idBookingTransports""")
    List<AppliedVoucher> findAppliedVouchersWithVoucherByBookingTransportIds(@Param("idBookingTransports") List<UUID> idBookingTransports);
}