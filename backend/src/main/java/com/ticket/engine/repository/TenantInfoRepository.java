package com.ticket.engine.repository;

import com.ticket.engine.entity.TenantInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantInfoRepository extends JpaRepository<TenantInfo, Long> {

    Optional<TenantInfo> findByCode(String code);
}
