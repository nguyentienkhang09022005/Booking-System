package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Dto.Request.AccommodationFilterRequest;
import com.example.java_service.Models.Accommodation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

@Repository
public class AccommodationFilterRepositoryImpl implements AccommodationFilterRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Accommodation> findAllWithFilters(AccommodationFilterRequest request, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Accommodation> query = cb.createQuery(Accommodation.class);
        Root<Accommodation> root = query.from(Accommodation.class);

        root.fetch("users", JoinType.LEFT);

        List<Predicate> predicates = buildPredicates(request, cb, root);
        query.where(predicates.toArray(new Predicate[0])).distinct(true);

        List<Accommodation> result = em.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Accommodation> countRoot = countQuery.from(Accommodation.class);
        countQuery.select(cb.count(countRoot)).where(buildPredicates(request, cb, countRoot).toArray(new Predicate[0]));

        long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(result, pageable, total);
    }

    @Override
    public List<Accommodation> findAllByIdIn(List<UUID> ids) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Accommodation> query = cb.createQuery(Accommodation.class);
        Root<Accommodation> root = query.from(Accommodation.class);
        root.fetch("users", JoinType.LEFT);
        query.where(root.get("idAccommodation").in(ids)).distinct(true);
        return em.createQuery(query).getResultList();
    }

    private List<Predicate> buildPredicates(AccommodationFilterRequest request, CriteriaBuilder cb, Root<Accommodation> root) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(root.get("approved"), true));

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = "%" + request.getKeyword().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("accommodationName")), keyword),
                    cb.like(cb.lower(root.get("description")), keyword),
                    cb.like(cb.lower(root.get("street")), keyword)
            ));
        }

        if (request.getAccommodationType() != null && !request.getAccommodationType().isBlank()) {
            predicates.add(cb.equal(cb.upper(root.get("accommodationType")), request.getAccommodationType().toUpperCase()));
        }

        if (request.getProvince() != null && !request.getProvince().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("province")), "%" + request.getProvince().toLowerCase() + "%"));
        }

        if (request.getCity() != null && !request.getCity().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("city")), "%" + request.getCity().toLowerCase() + "%"));
        }

        if (request.getWard() != null && !request.getWard().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("ward")), "%" + request.getWard().toLowerCase() + "%"));
        }

        if (request.getMinPrice() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("price"), request.getMinPrice()));
        }

        if (request.getMaxPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("price"), request.getMaxPrice()));
        }

        return predicates;
    }
}