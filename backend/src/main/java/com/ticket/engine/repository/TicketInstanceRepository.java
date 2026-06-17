package com.ticket.engine.repository;

import com.ticket.engine.entity.TicketInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketInstanceRepository extends JpaRepository<TicketInstance, Long> {

    Page<TicketInstance> findByTenantId(Long tenantId, Pageable pageable);

    Page<TicketInstance> findByTenantIdAndCurrentStateId(Long tenantId, String currentStateId, Pageable pageable);

    Optional<TicketInstance> findByTenantIdAndBusinessKey(Long tenantId, String businessKey);

    Optional<TicketInstance> findByIdAndTenantId(Long id, Long tenantId);
}
