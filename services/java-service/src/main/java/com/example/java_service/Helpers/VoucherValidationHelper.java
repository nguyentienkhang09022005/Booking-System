package com.example.java_service.Helpers;

import com.example.java_service.Constant.PredefinedApplyTo;
import com.example.java_service.Constant.PredefinedSponsorType;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Models.Voucher;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoucherValidationHelper {

    public void validateVouchersForAccommodation(List<Voucher> vouchers, UUID accommodationId, UUID accommodationOwnerUserId) {
        if (vouchers == null || vouchers.isEmpty()) return;
        for (Voucher voucher : vouchers) {
            validateVoucherForAccommodation(voucher, accommodationId, accommodationOwnerUserId);
        }
    }

    public void validateVoucherForAccommodation(Voucher voucher, UUID accommodationId, UUID accommodationOwnerUserId) {
        if (voucher == null) return;

        String sponsorType = getSponsorType(voucher);
        String applyTo = getApplyTo(voucher);

        if (PredefinedSponsorType.SYSTEM_TYPE.equals(sponsorType)) {
            return;
        }

        if (PredefinedSponsorType.BUSINESS_TYPE.equals(sponsorType)) {
            if (PredefinedApplyTo.APPLY_TO_TOUR.equals(applyTo) || PredefinedApplyTo.APPLY_TO_TICKET.equals(applyTo)) {
                throw new ApiException(ErrorCode.VOUCHER_NOT_APPLICABLE);
            }

            UUID voucherOwnerId = getVoucherOwnerUserId(voucher);
            if (voucherOwnerId == null || !voucherOwnerId.equals(accommodationOwnerUserId)) {
                throw new ApiException(ErrorCode.VOUCHER_NOT_BELONG_TO_ACCOMMODATION);
            }
        }
    }

    public void validateVouchersForTour(List<Voucher> vouchers, UUID tourOwnerUserId) {
        if (vouchers == null || vouchers.isEmpty()) return;
        for (Voucher voucher : vouchers) {
            validateVoucherForTour(voucher, tourOwnerUserId);
        }
    }

    public void validateVoucherForTour(Voucher voucher, UUID tourOwnerUserId) {
        if (voucher == null) return;

        String sponsorType = getSponsorType(voucher);
        String applyTo = getApplyTo(voucher);

        if (PredefinedSponsorType.SYSTEM_TYPE.equals(sponsorType)) {
            return;
        }

        if (PredefinedSponsorType.BUSINESS_TYPE.equals(sponsorType)) {
            if (PredefinedApplyTo.APPLY_TO_ROOM.equals(applyTo) || PredefinedApplyTo.APPLY_TO_SERVICE.equals(applyTo)) {
                throw new ApiException(ErrorCode.VOUCHER_NOT_APPLICABLE);
            }

            UUID voucherOwnerId = getVoucherOwnerUserId(voucher);
            if (voucherOwnerId == null || !voucherOwnerId.equals(tourOwnerUserId)) {
                throw new ApiException(ErrorCode.VOUCHER_NOT_BELONG_TO_TOUR);
            }
        }
    }

    public void validateVouchersForTransportTicket(List<Voucher> vouchers, UUID ticketOwnerUserId) {
        if (vouchers == null || vouchers.isEmpty()) return;
        for (Voucher voucher : vouchers) {
            validateVoucherForTransportTicket(voucher, ticketOwnerUserId);
        }
    }

    public void validateVoucherForTransportTicket(Voucher voucher, UUID ticketOwnerUserId) {
        if (voucher == null) return;

        String sponsorType = getSponsorType(voucher);
        String applyTo = getApplyTo(voucher);

        if (PredefinedSponsorType.SYSTEM_TYPE.equals(sponsorType)) {
            return;
        }

        if (PredefinedSponsorType.BUSINESS_TYPE.equals(sponsorType)) {
            if (PredefinedApplyTo.APPLY_TO_ROOM.equals(applyTo) || PredefinedApplyTo.APPLY_TO_SERVICE.equals(applyTo)) {
                throw new ApiException(ErrorCode.VOUCHER_NOT_APPLICABLE);
            }

            UUID voucherOwnerId = getVoucherOwnerUserId(voucher);
            if (voucherOwnerId == null || !voucherOwnerId.equals(ticketOwnerUserId)) {
                throw new ApiException(ErrorCode.VOUCHER_NOT_BELONG_TO_TRANSPORT);
            }
        }
    }

    private String getSponsorType(Voucher voucher) {
        return voucher.getSponsorType() != null ? voucher.getSponsorType().toUpperCase() : PredefinedSponsorType.SYSTEM_TYPE;
    }

    private String getApplyTo(Voucher voucher) {
        return voucher.getApplyTo() != null ? voucher.getApplyTo().toUpperCase() : PredefinedApplyTo.APPLY_TO_ALL;
    }

    private UUID getVoucherOwnerUserId(Voucher voucher) {
        if (voucher.getAccommodation() != null && voucher.getAccommodation().getUsers() != null) {
            return voucher.getAccommodation().getUsers().getIdUser();
        }
        return null;
    }

    public Map<UUID, Voucher> mapVouchersById(List<Voucher> vouchers) {
        if (vouchers == null || vouchers.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<UUID, Voucher> map = new HashMap<>();
        for (Voucher v : vouchers) {
            map.put(v.getIdVoucher(), v);
        }
        return map;
    }
}