package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Dto.Request.TransportTicketFilterRequest;
import com.example.java_service.Models.TransportTicket;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

@Repository
public class TransportTicketFilterRepositoryImpl implements TransportTicketFilterRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<TransportTicket> findAllWithFilters(TransportTicketFilterRequest request, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<TransportTicket> query = cb.createQuery(TransportTicket.class);
        Root<TransportTicket> root = query.from(TransportTicket.class);

        root.fetch("users", JoinType.LEFT);

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

    private List<Predicate> buildPredicates(TransportTicketFilterRequest request, CriteriaBuilder cb, Root<TransportTicket> root) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(root.get("approved"), true));

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = "%" + request.getKeyword().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("providerName")), keyword),
                    cb.like(cb.lower(root.get("ticketType")), keyword),
                    cb.like(cb.lower(root.get("departureLocation")), keyword),
                    cb.like(cb.lower(root.get("arrivalLocation")), keyword)
            ));
        }

        if (request.getTicketType() != null && !request.getTicketType().isBlank()) {
            predicates.add(cb.equal(cb.upper(root.get("ticketType")), request.getTicketType().toUpperCase()));
        }

        if (request.getTicketClass() != null && !request.getTicketClass().isBlank()) {
            predicates.add(cb.equal(cb.upper(root.get("ticketClass")), request.getTicketClass().toUpperCase()));
        }

        if (request.getProviderName() != null && !request.getProviderName().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("providerName")), "%" + request.getProviderName().toLowerCase() + "%"));
        }

        if (request.getDepartureLocation() != null && !request.getDepartureLocation().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("departureLocation")), "%" + request.getDepartureLocation().toLowerCase() + "%"));
        }

        if (request.getArrivalLocation() != null && !request.getArrivalLocation().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("arrivalLocation")), "%" + request.getArrivalLocation().toLowerCase() + "%"));
        }

        if (request.getDepartureFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("departureTime"), request.getDepartureFrom()));
        }

        if (request.getDepartureTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("departureTime"), request.getDepartureTo()));
        }

        if (request.getArrivalFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("arrivalTime"), request.getArrivalFrom()));
        }

        if (request.getArrivalTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("arrivalTime"), request.getArrivalTo()));
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