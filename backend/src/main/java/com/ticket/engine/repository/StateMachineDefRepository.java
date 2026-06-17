package com.ticket.engine.repository;

import com.ticket.engine.entity.StateMachineDef;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StateMachineDefRepository extends JpaRepository<StateMachineDef, Long> {

    List<StateMachineDef> findByTenantId(Long tenantId);

    List<StateMachineDef> findByTenantIdAndStatus(Long tenantId, String status);

    Page<StateMachineDef> findByTenantId(Long tenantId, Pageable pageable);

    Optional<StateMachineDef> findByIdAndTenantId(Long id, Long tenantId);
}
