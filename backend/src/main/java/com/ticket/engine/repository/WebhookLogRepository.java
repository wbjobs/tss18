package com.ticket.engine.repository;

import com.ticket.engine.entity.WebhookLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, Long> {

    Page<WebhookLog> findByWebhookIdOrderByCreatedAtDesc(Long webhookId, Pageable pageable);

    List<WebhookLog> findByStatusOrderByCreatedAtAsc(String status);
}
