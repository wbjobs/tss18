package com.ticket.engine.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "saga_compensation_log")
@EntityListeners(AuditingEntityListener.class)
public class SagaCompensationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ticketId;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 50)
    private String sagaId;

    @Column(nullable = false, length = 50)
    private String step;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, length = 50)
    private String fromStateId;

    @Column(nullable = false, length = 100)
    private String fromStateName;

    @Column(nullable = false, length = 50)
    private String toStateId;

    @Column(nullable = false, length = 100)
    private String toStateName;

    @Column(length = 50)
    private String transitionId;

    @Column(length = 20)
    private String triggerSource;

    @Column(columnDefinition = "TEXT")
    private String snapshotPayload;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false, length = 50)
    private Long snapshotVersion;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime compensatedAt;
}
