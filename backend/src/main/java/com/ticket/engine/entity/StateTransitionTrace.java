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
@Table(name = "state_transition_trace")
@EntityListeners(AuditingEntityListener.class)
public class StateTransitionTrace {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long ticketId;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 50)
    private String fromStateId;

    @Column(nullable = false, length = 100)
    private String fromStateName;

    @Column(nullable = false, length = 50)
    private String toStateId;

    @Column(nullable = false, length = 100)
    private String toStateName;

    @Column(nullable = false)
    private Long operatorId;

    @Column(nullable = false, length = 100)
    private String operatorName;

    @Column(nullable = false, length = 20)
    private String triggerSource;

    @Column(length = 500)
    private String remark;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
