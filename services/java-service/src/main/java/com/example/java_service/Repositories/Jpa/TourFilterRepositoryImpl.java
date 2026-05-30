package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Dto.Request.TourFilterRequest;
import com.example.java_service.Models.Tour;
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

@Repository
public class TourFilterRepositoryImpl implements TourFilterRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Tour> findAllWithFilters(TourFilterRequest request, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tour> query = cb.createQuery(Tour.class);
        Root<Tour> root = query.from(Tour.class);

        root.fetch("tourSchedules", JoinType.LEFT);

        List<Predicate> predicates = buildPredicates(request, cb, root);
        query.where(predicates.toArray(new Predicate[0])).distinct(true);

        List<Tour> result = em.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Tour> countRoot = countQuery.from(Tour.class);
        countQuery.select(cb.count(countRoot)).where(buildPredicates(request, cb, countRoot).toArray(new Predicate[0]));

        long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(result, pageable, total);
    }

    private List<Predicate> buildPredicates(TourFilterRequest request, CriteriaBuilder cb, Root<Tour> root) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(root.get("approved"), true));

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = "%" + request.getKeyword().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("tourName")), keyword),
                    cb.like(cb.lower(root.get("description")), keyword),
                    cb.like(cb.lower(root.get("meetingPoint")), keyword),
                    cb.like(cb.lower(root.get("dropOffPoint")), keyword)
            ));
        }

        if (request.getMinAdultPrice() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("adultPrice"), request.getMinAdultPrice()));
        }

        if (request.getMaxAdultPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("adultPrice"), request.getMaxAdultPrice()));
        }

        if (request.getMinChildPrice() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("childPrice"), request.getMinChildPrice()));
        }

        if (request.getMaxChildPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("childPrice"), request.getMaxChildPrice()));
        }

        if (request.getMinDuration() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("durationMinutes"), request.getMinDuration()));
        }

        if (request.getMaxDuration() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("durationMinutes"), request.getMaxDuration()));
        }

        if (request.getLanguages() != null && !request.getLanguages().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("languages")), "%" + request.getLanguages().toLowerCase() + "%"));
        }

        if (request.getDepartureLocation() != null && !request.getDepartureLocation().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("meetingPoint")), "%" + request.getDepartureLocation().toLowerCase() + "%"));
        }

        if (request.getDestination() != null && !request.getDestination().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("dropOffPoint")), "%" + request.getDestination().toLowerCase() + "%"));
        }

        return predicates;
    }
}