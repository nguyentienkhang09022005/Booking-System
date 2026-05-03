package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Dto.Request.AccommodationSearchRequest;
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
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class AccommodationSearchRepositoryImpl implements AccommodationSearchRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Accommodation> searchWithKeyword(AccommodationSearchRequest request, Pageable pageable) {
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

    private List<Predicate> buildPredicates(AccommodationSearchRequest request, CriteriaBuilder cb, Root<Accommodation> root) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(root.get("approved"), true));

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().trim().toLowerCase();

            Predicate nameMatch = cb.like(cb.lower(root.get("accommodationName")), "%" + keyword + "%");
            Predicate cityMatch = cb.like(cb.lower(root.get("city")), "%" + keyword + "%");
            Predicate typeMatch = cb.like(cb.lower(root.get("accommodationType")), "%" + keyword + "%");
            Predicate provinceMatch = cb.like(cb.lower(root.get("province")), "%" + keyword + "%");

            predicates.add(cb.or(nameMatch, cityMatch, typeMatch, provinceMatch));
        }

        return predicates;
    }
}