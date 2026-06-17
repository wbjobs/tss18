package com.ticket.engine.service;

import com.ticket.engine.dto.PageResult;
import com.ticket.engine.engine.TenantContext;
import com.ticket.engine.entity.StateTransitionTrace;
import com.ticket.engine.entity.TicketInstance;
import com.ticket.engine.repository.StateTransitionTraceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TraceService {

    @Autowired
    private StateTransitionTraceRepository stateTransitionTraceRepository;

    @Transactional
    public StateTransitionTrace createTrace(TicketInstance ticket, String fromStateId, String fromStateName,
                                            String toStateId, String toStateName, String triggerSource,
                                            String remark, Long operatorId, String operatorName) {
        if (ticket == null) {
            throw new RuntimeException("工单实例不能为空");
        }

        StateTransitionTrace trace = StateTransitionTrace.builder()
                .id(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE)
                .ticketId(ticket.getId())
                .tenantId(ticket.getTenantId())
                .fromStateId(fromStateId)
                .fromStateName(fromStateName)
                .toStateId(toStateId)
                .toStateName(toStateName)
                .triggerSource(triggerSource)
                .remark(remark)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .build();

        return stateTransitionTraceRepository.save(trace);
    }

    @Transactional(readOnly = true)
    public List<StateTransitionTrace> getTicketTraces(Long ticketId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        if (ticketId == null) {
            throw new RuntimeException("工单ID不能为空");
        }

        return stateTransitionTraceRepository.findByTicketIdAndTenantIdOrderByCreatedAtAsc(ticketId, tenantId);
    }

    @Transactional(readOnly = true)
    public PageResult<StateTransitionTrace> listTraces(int page, int size) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StateTransitionTrace> pageResult = stateTransitionTraceRepository.findByTenantId(tenantId, pageable);

        return PageResult.<StateTransitionTrace>builder()
                .list(pageResult.getContent())
                .total(pageResult.getTotalElements())
                .build();
    }
}
