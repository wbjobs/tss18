package com.ticket.engine.config;

import com.ticket.engine.engine.TenantContext;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String CALLBACK_PATH_PATTERN = "/api/callback/";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestPath = request.getRequestURI();
            if (!requestPath.startsWith(CALLBACK_PATH_PATTERN)) {
                String tenantIdStr = request.getHeader(TENANT_ID_HEADER);
                if (tenantIdStr != null && !tenantIdStr.isEmpty()) {
                    Long tenantId = Long.parseLong(tenantIdStr);
                    TenantContext.setCurrentTenantId(tenantId);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
