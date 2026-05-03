package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Dto.Request.TransportTicketSearchRequest;
import com.example.java_service.Models.TransportTicket;
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
public class TransportTicketSearchRepositoryImpl implements TransportTicketSearchRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<TransportTicket> searchWithKeyword(TransportTicketSearchRequest request, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<TransportTicket> query = cb.createQuery(TransportTicket.class);
        Root<TransportTicket> root = query.from(TransportTicket.class);

        List<Predicate> predicates = buildPredicates(request, cb, root);
        query.where(predicates.toArray(new Predicate[0])).distinct(true);

        List<TransportTicket> result = em.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<TransportTicket> countRoot = countQuery.from(TransportTicket.class);
        countQuery.select(cb.count(countRoot)).where(buildPredicates(request, cb, countRoot).toArray(new Predicate[0]));

        long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(result, pageable, total);
    }

    private List<Predicate> buildPredicates(TransportTicketSearchRequest request, CriteriaBuilder cb, Root<TransportTicket> root) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(root.get("approved"), true));

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().trim().toLowerCase();

            Predicate providerMatch = cb.like(cb.lower(root.get("providerName")), "%" + keyword + "%");
            Predicate departureMatch = cb.like(cb.lower(root.get("departureLocation")), "%" + keyword + "%");
            Predicate arrivalMatch = cb.like(cb.lower(root.get("arrivalLocation")), "%" + keyword + "%");
            Predicate ticketTypeMatch = cb.like(cb.lower(root.get("ticketType")), "%" + keyword + "%");

            predicates.add(cb.or(providerMatch, departureMatch, arrivalMatch, ticketTypeMatch));
        }

        return predicates;
    }
}