package com.ticket.engine.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "webhook_config")
@EntityListeners(AuditingEntityListener.class)
public class WebhookConfig {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false, length = 100)
    private String secretKey;

    @Column(nullable = false, length = 200)
    private String events;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(nullable = false)
    private Integer retryCount;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
