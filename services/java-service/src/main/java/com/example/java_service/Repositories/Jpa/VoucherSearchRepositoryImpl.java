package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Dto.Request.VoucherSearchRequest;
import com.example.java_service.Models.Voucher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class VoucherSearchRepositoryImpl implements VoucherSearchRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Voucher> searchWithKeyword(VoucherSearchRequest request, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Voucher> query = cb.createQuery(Voucher.class);
        Root<Voucher> root = query.from(Voucher.class);

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

    private List<Predicate> buildPredicates(VoucherSearchRequest request, CriteriaBuilder cb, Root<Voucher> root) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(root.get("approved"), true));

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().trim().toLowerCase();

            Predicate nameMatch = cb.like(cb.lower(root.get("nameVoucher")), "%" + keyword + "%");
            Predicate sponsorMatch = cb.like(cb.lower(root.get("sponsorType")), "%" + keyword + "%");
            Predicate applyMatch = cb.like(cb.lower(root.get("applyTo")), "%" + keyword + "%");

            predicates.add(cb.or(nameMatch, sponsorMatch, applyMatch));
        }

        return predicates;
    }
}