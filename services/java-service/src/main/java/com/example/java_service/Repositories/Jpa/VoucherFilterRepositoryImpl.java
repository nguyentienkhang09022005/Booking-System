package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Dto.Request.VoucherFilterRequest;
import com.example.java_service.Models.Accommodation;
import com.example.java_service.Models.Voucher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

@Repository
public class VoucherFilterRepositoryImpl implements VoucherFilterRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Voucher> findAllWithFilters(VoucherFilterRequest request, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Voucher> query = cb.createQuery(Voucher.class);
        Root<Voucher> root = query.from(Voucher.class);

        Join<Voucher, Accommodation> accommodationJoin = root.join("accommodation", JoinType.LEFT);
        accommodationJoin.join("users", JoinType.LEFT);

        List<Predicate> predicates = buildPredicates(request, cb, root);
        query.where(predicates.toArray(new Predicate[0])).distinct(true);

        List<Voucher> result = em.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Voucher> countRoot = countQuery.from(Voucher.class);
        countQuery.select(cb.count(countRoot)).where(buildPredicates(request, cb, countRoot).toArray(new Predicate[0]));

        long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(result, pageable, total);
    }

    private List<Predicate> buildPredicates(VoucherFilterRequest request, CriteriaBuilder cb, Root<Voucher> root) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(root.get("approved"), true));

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = "%" + request.getKeyword().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("nameVoucher")), keyword),
                    cb.like(cb.lower(root.get("conditions")), keyword)
            ));
        }

        if (request.getCode() != null && !request.getCode().isBlank()) {
            predicates.add(cb.equal(cb.upper(root.get("code")), request.getCode().toUpperCase()));
        }

        if (request.getMinDiscountPercent() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("discountPercent"), request.getMinDiscountPercent()));
        }

        if (request.getMaxDiscountPercent() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("discountPercent"), request.getMaxDiscountPercent()));
        }

        if (request.getMinDiscountAmount() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("minDiscountAmount"), request.getMinDiscountAmount()));
        }

        if (request.getMaxDiscountAmount() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("maxDiscountAmount"), request.getMaxDiscountAmount()));
        }

        if (request.getSponsorType() != null && !request.getSponsorType().isBlank()) {
            predicates.add(cb.equal(cb.upper(root.get("sponsorType")), request.getSponsorType().toUpperCase()));
        }

        if (request.getApplyTo() != null && !request.getApplyTo().isBlank()) {
            predicates.add(cb.equal(cb.upper(root.get("applyTo")), request.getApplyTo().toUpperCase()));
        }

        if (request.getValidFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), request.getValidFrom()));
        }

        if (request.getValidTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), request.getValidTo()));
        }

        if (request.getMinQuantity() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("quantity"), request.getMinQuantity()));
        }

        if (request.getAccommodationId() != null) {
            predicates.add(cb.equal(root.get("accommodation").get("idAccommodation"), request.getAccommodationId()));
        }

        if (Boolean.TRUE.equals(request.getExpireSoon())) {
            predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), LocalDate.now().plusDays(7)));
        }

        if (Boolean.TRUE.equals(request.getNewVoucher())) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createAt"), LocalDate.now().minusDays(7).atStartOfDay()));
        }

        return predicates;
    }
}