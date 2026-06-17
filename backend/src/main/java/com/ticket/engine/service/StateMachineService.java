package com.ticket.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.engine.dto.CreateStateMachineRequest;
import com.ticket.engine.dto.PageResult;
import com.ticket.engine.dto.StateMachineDefinition;
import com.ticket.engine.dto.UpdateStateMachineRequest;
import com.ticket.engine.engine.StateMachineCache;
import com.ticket.engine.engine.StateMachineEngine;
import com.ticket.engine.engine.TenantContext;
import com.ticket.engine.entity.StateMachineDef;
import com.ticket.engine.repository.StateMachineDefRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class StateMachineService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_OFFLINE = "OFFLINE";

    @Autowired
    private StateMachineDefRepository stateMachineDefRepository;

    @Autowired
    private StateMachineCache stateMachineCache;

    @Autowired
    private StateMachineEngine stateMachineEngine;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public StateMachineDef createStateMachine(CreateStateMachineRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        StateMachineDefinition definition = StateMachineDefinition.builder()
                .nodes(new ArrayList<>())
                .transitions(new ArrayList<>())
                .build();

        String definitionJson;
        try {
            definitionJson = objectMapper.writeValueAsString(definition);
        } catch (Exception e) {
            throw new RuntimeException("序列化状态机定义失败", e);
        }

        StateMachineDef stateMachineDef = StateMachineDef.builder()
                .id(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE)
                .tenantId(tenantId)
                .name(request.getName())
                .description(request.getDescription())
                .definitionJson(definitionJson)
                .version(1)
                .status(STATUS_DRAFT)
                .build();

        return stateMachineDefRepository.save(stateMachineDef);
    }

    @Transactional
    public StateMachineDef updateStateMachine(Long id, UpdateStateMachineRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        StateMachineDef stateMachineDef = stateMachineDefRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("状态机不存在"));

        if (STATUS_PUBLISHED.equals(stateMachineDef.getStatus())) {
            throw new RuntimeException("已发布的状态机不能修改，请先下线");
        }

        StateMachineDefinition definition = StateMachineDefinition.builder()
                .nodes(request.getNodes())
                .transitions(request.getTransitions())
                .build();

        String definitionJson;
        try {
            definitionJson = objectMapper.writeValueAsString(definition);
        } catch (Exception e) {
            throw new RuntimeException("序列化状态机定义失败", e);
        }

        stateMachineDef.setDefinitionJson(definitionJson);
        stateMachineDef.setVersion(stateMachineDef.getVersion() + 1);

        return stateMachineDefRepository.save(stateMachineDef);
    }

    @Transactional
    public StateMachineDef publishStateMachine(Long id) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        StateMachineDef stateMachineDef = stateMachineDefRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("状态机不存在"));

        stateMachineDef.setStatus(STATUS_PUBLISHED);
        StateMachineDef saved = stateMachineDefRepository.save(stateMachineDef);

        loadStateMachineDefinition(tenantId, id);

        return saved;
    }

    @Transactional
    public StateMachineDef offlineStateMachine(Long id) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        StateMachineDef stateMachineDef = stateMachineDefRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("状态机不存在"));

        stateMachineDef.setStatus(STATUS_OFFLINE);
        StateMachineDef saved = stateMachineDefRepository.save(stateMachineDef);

        stateMachineCache.evict(tenantId, id);

        return saved;
    }

    @Transactional(readOnly = true)
    public StateMachineDef getStateMachine(Long id) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        StateMachineDef stateMachineDef = stateMachineDefRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("状态机不存在"));

        return stateMachineDef;
    }

    @Transactional(readOnly = true)
    public StateMachineDefinition getStateMachineDefinition(Long id) {
        StateMachineDef stateMachineDef = getStateMachine(id);
        try {
            return objectMapper.readValue(stateMachineDef.getDefinitionJson(), StateMachineDefinition.class);
        } catch (Exception e) {
            throw new RuntimeException("反序列化状态机定义失败", e);
        }
    }

    @Transactional(readOnly = true)
    public PageResult<StateMachineDef> listStateMachines(int page, int size) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StateMachineDef> pageResult = stateMachineDefRepository.findByTenantId(tenantId, pageable);

        return PageResult.<StateMachineDef>builder()
                .list(pageResult.getContent())
                .total(pageResult.getTotalElements())
                .build();
    }

    @Transactional
    public void deleteStateMachine(Long id) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        StateMachineDef stateMachineDef = stateMachineDefRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("状态机不存在"));

        stateMachineDefRepository.delete(stateMachineDef);
        stateMachineCache.evict(tenantId, id);
    }

    @Transactional(readOnly = true)
    public StateMachineDefinition loadStateMachineDefinition(Long tenantId, Long stateMachineId) {
        if (tenantId == null || stateMachineId == null) {
            throw new RuntimeException("参数不能为空");
        }

        StateMachineDefinition cached = stateMachineCache.get(tenantId, stateMachineId);
        if (cached != null) {
            return cached;
        }

        StateMachineDef stateMachineDef = stateMachineDefRepository.findByIdAndTenantId(stateMachineId, tenantId)
                .orElseThrow(() -> new RuntimeException("状态机不存在"));

        if (!STATUS_PUBLISHED.equals(stateMachineDef.getStatus())) {
            throw new RuntimeException("状态机未发布");
        }

        StateMachineDefinition definition;
        try {
            definition = objectMapper.readValue(stateMachineDef.getDefinitionJson(), StateMachineDefinition.class);
        } catch (Exception e) {
            throw new RuntimeException("反序列化状态机定义失败", e);
        }

        stateMachineCache.put(tenantId, stateMachineId, definition);

        return definition;
    }
}
