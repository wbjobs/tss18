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
@Table(name = "ticket_instance")
@EntityListeners(AuditingEntityListener.class)
public class TicketInstance {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private Long stateMachineId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String businessKey;

    @Column(nullable = false, length = 50)
    private String currentStateId;

    @Column(nullable = false, length = 100)
    private String currentStateName;

    @Column(columnDefinition = "JSON")
    private String payload;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
