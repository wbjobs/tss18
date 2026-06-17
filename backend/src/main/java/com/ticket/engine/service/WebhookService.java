package com.ticket.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.engine.dto.PageResult;
import com.ticket.engine.dto.WebhookConfigRequest;
import com.ticket.engine.engine.TenantContext;
import com.ticket.engine.entity.WebhookConfig;
import com.ticket.engine.entity.WebhookLog;
import com.ticket.engine.repository.WebhookConfigRepository;
import com.ticket.engine.repository.WebhookLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class WebhookService {

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    @Autowired
    private WebhookConfigRepository webhookConfigRepository;

    @Autowired
    private WebhookLogRepository webhookLogRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public WebhookConfig createWebhookConfig(WebhookConfigRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        String secretKey = request.getSecretKey();
        if (secretKey == null || secretKey.trim().isEmpty()) {
            secretKey = generateSecretKey();
        }

        String events = String.join(",", request.getEvents());

        WebhookConfig config = WebhookConfig.builder()
                .id(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE)
                .tenantId(tenantId)
                .name(request.getName())
                .url(request.getUrl())
                .secretKey(secretKey)
                .events(events)
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .retryCount(request.getRetryCount() != null ? request.getRetryCount() : 3)
                .build();

        return webhookConfigRepository.save(config);
    }

    @Transactional
    public WebhookConfig updateWebhookConfig(Long id, WebhookConfigRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        WebhookConfig config = webhookConfigRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Webhook配置不存在"));

        if (request.getName() != null) {
            config.setName(request.getName());
        }
        if (request.getUrl() != null) {
            config.setUrl(request.getUrl());
        }
        if (request.getSecretKey() != null) {
            config.setSecretKey(request.getSecretKey());
        }
        if (request.getEvents() != null) {
            config.setEvents(String.join(",", request.getEvents()));
        }
        if (request.getEnabled() != null) {
            config.setEnabled(request.getEnabled());
        }
        if (request.getRetryCount() != null) {
            config.setRetryCount(request.getRetryCount());
        }

        return webhookConfigRepository.save(config);
    }

    @Transactional
    public void deleteWebhookConfig(Long id) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        WebhookConfig config = webhookConfigRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Webhook配置不存在"));

        webhookConfigRepository.delete(config);
    }

    @Transactional(readOnly = true)
    public PageResult<WebhookConfig> listWebhookConfigs(int page, int size) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<WebhookConfig> pageResult = webhookConfigRepository.findByTenantId(tenantId, pageable);

        return PageResult.<WebhookConfig>builder()
                .list(pageResult.getContent())
                .total(pageResult.getTotalElements())
                .build();
    }

    @Transactional(readOnly = true)
    public WebhookConfig getWebhookConfig(Long id) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        return webhookConfigRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Webhook配置不存在"));
    }

    @Async
    @Transactional
    public void notifyWebhooks(Long tenantId, String event, Object payload) {
        if (tenantId == null || event == null) {
            return;
        }

        List<WebhookConfig> configs = webhookConfigRepository.findByTenantIdAndEnabledTrue(tenantId);
        if (configs == null || configs.isEmpty()) {
            return;
        }

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("序列化payload失败", e);
        }

        for (WebhookConfig config : configs) {
            if (!isEventMatched(config.getEvents(), event)) {
                continue;
            }

            sendWebhookWithRetry(config, event, payloadJson);
        }
    }

    @Transactional
    public boolean testWebhook(Long id) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        WebhookConfig config = webhookConfigRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Webhook配置不存在"));

        Map<String, Object> testPayload = new HashMap<>();
        testPayload.put("test", true);
        testPayload.put("message", "Webhook测试");
        testPayload.put("timestamp", System.currentTimeMillis());

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(testPayload);
        } catch (Exception e) {
            throw new RuntimeException("序列化测试payload失败", e);
        }

        WebhookLog log = sendWebhookWithRetry(config, "TEST", payloadJson);
        return STATUS_SUCCESS.equals(log.getStatus());
    }

    @Transactional(readOnly = true)
    public PageResult<WebhookLog> listWebhookLogs(Long webhookId, int page, int size) {
        if (webhookId == null) {
            throw new RuntimeException("Webhook ID不能为空");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<WebhookLog> pageResult = webhookLogRepository.findByWebhookIdOrderByCreatedAtDesc(webhookId, pageable);

        return PageResult.<WebhookLog>builder()
                .list(pageResult.getContent())
                .total(pageResult.getTotalElements())
                .build();
    }

    public boolean verifySignature(String secretKey, String payload, String signature) {
        if (secretKey == null || payload == null || signature == null) {
            return false;
        }

        String expectedSignature = generateSignature(secretKey, payload);
        return MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateSignature(String secretKey, String payload) {
        if (secretKey == null || payload == null) {
            throw new RuntimeException("参数不能为空");
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("生成签名失败", e);
        }
    }

    private String generateSecretKey() {
        byte[] randomBytes = new byte[32];
        new Random().nextBytes(randomBytes);
        return bytesToHex(randomBytes);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private boolean isEventMatched(String events, String targetEvent) {
        if (events == null || targetEvent == null) {
            return false;
        }
        List<String> eventList = Arrays.asList(events.split(","));
        return eventList.contains(targetEvent) || eventList.contains("*");
    }

    private WebhookLog sendWebhookWithRetry(WebhookConfig config, String event, String payloadJson) {
        int maxRetry = config.getRetryCount() != null ? config.getRetryCount() : 3;
        int retryTimes = 0;
        String status = STATUS_FAILED;
        String response = null;

        while (retryTimes < maxRetry) {
            try {
                response = sendWebhook(config, event, payloadJson);
                status = STATUS_SUCCESS;
                break;
            } catch (Exception e) {
                retryTimes++;
                response = e.getMessage();
                if (retryTimes < maxRetry) {
                    try {
                        Thread.sleep(1000 * retryTimes);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        WebhookLog log = WebhookLog.builder()
                .id(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE)
                .webhookId(config.getId())
                .url(config.getUrl())
                .event(event)
                .payload(payloadJson)
                .status(status)
                .response(response)
                .retryTimes(retryTimes)
                .build();

        return webhookLogRepository.save(log);
    }

    private String sendWebhook(WebhookConfig config, String event, String payloadJson) {
        String signature = generateSignature(config.getSecretKey(), payloadJson);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Webhook-Event", event);
        headers.set("X-Webhook-Signature", signature);
        headers.set("X-Webhook-Timestamp", String.valueOf(System.currentTimeMillis()));

        HttpEntity<String> requestEntity = new HttpEntity<>(payloadJson, headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange(
                config.getUrl(),
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("HTTP错误: " + responseEntity.getStatusCodeValue());
        }

        return responseEntity.getBody();
    }
}
