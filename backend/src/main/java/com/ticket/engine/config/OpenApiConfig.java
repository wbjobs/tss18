package com.ticket.engine.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String TENANT_ID_HEADER = "X-Tenant-Id";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ticket Engine API")
                        .version("1.0.0")
                        .description("Ticket Engine Backend Service API Documentation"))
                .addSecurityItem(new SecurityRequirement().addList(TENANT_ID_HEADER))
                .components(new Components()
                        .addSecuritySchemes(TENANT_ID_HEADER, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(TENANT_ID_HEADER)
                                .description("Tenant ID for multi-tenant isolation")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/**")
                .addOpenApiCustomiser(globalHeaderCustomiser())
                .build();
    }

    private OpenApiCustomiser globalHeaderCustomiser() {
        return openApi -> openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
            if (operation.getParameters() == null ||
                    operation.getParameters().stream().noneMatch(p -> TENANT_ID_HEADER.equalsIgnoreCase(p.getName()))) {
                operation.addParametersItem(new Parameter()
                        .in("header")
                        .name(TENANT_ID_HEADER)
                        .schema(new StringSchema())
                        .required(false)
                        .description("Tenant ID for multi-tenant isolation"));
            }
        }));
    }
}
