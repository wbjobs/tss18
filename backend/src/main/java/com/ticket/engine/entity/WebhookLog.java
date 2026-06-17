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
@Table(name = "webhook_log")
@EntityListeners(AuditingEntityListener.class)
public class WebhookLog {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long webhookId;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false, length = 50)
    private String event;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String payload;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(columnDefinition = "LONGTEXT")
    private String response;

    @Column(nullable = false)
    private Integer retryTimes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
