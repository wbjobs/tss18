package com.ticket.engine.repository;

import com.ticket.engine.entity.SagaCompensationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SagaCompensationLogRepository extends JpaRepository<SagaCompensationLog, Long> {

    List<SagaCompensationLog> findByTicketIdOrderByCreatedAtDesc(Long ticketId);

    List<SagaCompensationLog> findBySagaIdOrderByCreatedAtAsc(String sagaId);

    List<SagaCompensationLog> findByStatusOrderByCreatedAtAsc(String status);
}
