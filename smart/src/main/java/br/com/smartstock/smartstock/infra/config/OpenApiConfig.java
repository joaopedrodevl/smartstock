package br.com.smartstock.smartstock.infra.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "cookieAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("SmartStock API")
                        .description("""
                                API para gerenciamento de mini mercados de condomínio

                                ## Autenticação
                                1. Faça login em `POST /v1/token/admin` para obter os cookies `access_token` e `XSRF-TOKEN`
                                2. No Swagger UI, clique em **Authorize** e cole: `access_token=SEU_TOKEN`
                                3. Para endpoints que modificam dados (POST, PUT, DELETE), envie o header `X-XSRF-TOKEN` com o valor do cookie XSRF-TOKEN

                                ## Headers Opcionais
                                - `X-Request-Id`: ID único para rastreamento da requisição (UUID recomendado). Útil para correlacionar logs em caso de erros.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SmartStock")
                                .email("contato@smartstock.com")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name("access_token")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .description("JWT token nos cookies - faça login em /v1/token/admin")));
    }

    @Bean
    public OperationCustomizer requestHeaderCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            Parameter requestId = new HeaderParameter()
                    .name("X-Request-Id")
                    .description("ID único para rastreamento da requisição (UUID recomendado)")
                    .required(false)
                    .schema(new Schema<String>().type("string").example("550e8400-e29b-41d4-a716-446655440000"));

            if (operation.getParameters() == null) {
                operation.setParameters(new ArrayList<>());
            }
            operation.getParameters().add(requestId);
            return operation;
        };
    }
}
