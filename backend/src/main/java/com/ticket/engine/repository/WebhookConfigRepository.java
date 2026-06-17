package com.ticket.engine.repository;

import com.ticket.engine.entity.WebhookConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, Long> {

    List<WebhookConfig> findByTenantIdAndEnabledTrue(Long tenantId);

    Optional<WebhookConfig> findByIdAndTenantId(Long id, Long tenantId);

    Page<WebhookConfig> findByTenantId(Long tenantId, Pageable pageable);
}
