package com.ticket.engine.repository;

import com.ticket.engine.entity.StateTransitionTrace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StateTransitionTraceRepository extends JpaRepository<StateTransitionTrace, Long> {

    List<StateTransitionTrace> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    List<StateTransitionTrace> findByTicketIdAndTenantIdOrderByCreatedAtAsc(Long ticketId, Long tenantId);

    Page<StateTransitionTrace> findByTenantId(Long tenantId, Pageable pageable);
}
