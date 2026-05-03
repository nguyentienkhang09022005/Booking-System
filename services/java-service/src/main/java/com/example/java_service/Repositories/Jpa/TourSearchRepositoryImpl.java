package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Dto.Request.TourSearchRequest;
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
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class TourSearchRepositoryImpl implements TourSearchRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Tour> searchWithKeyword(TourSearchRequest request, Pageable pageable) {
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

    private List<Predicate> buildPredicates(TourSearchRequest request, CriteriaBuilder cb, Root<Tour> root) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(root.get("approved"), true));

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().trim().toLowerCase();

            Predicate nameMatch = cb.like(cb.lower(root.get("tourName")), "%" + keyword + "%");
            Predicate dropOffMatch = cb.like(cb.lower(root.get("dropOffPoint")), "%" + keyword + "%");
            Predicate meetingMatch = cb.like(cb.lower(root.get("meetingPoint")), "%" + keyword + "%");

            predicates.add(cb.or(nameMatch, dropOffMatch, meetingMatch));
        }

        return predicates;
    }
}