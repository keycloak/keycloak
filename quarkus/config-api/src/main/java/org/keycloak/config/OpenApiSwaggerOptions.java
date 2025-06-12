package org.keycloak.config;

public class OpenApiSwaggerOptions {

    public static final Option<Boolean> OPENAPI_SWAGGER_ENABLED = new OptionBuilder<>("openapi-enabled", Boolean.class)
            .category(OptionCategory.OPENAPI_SWAGGER)
            .description("If the server should expose OpenAPI and Swagger-UI Endpoint. If enabled, OpenAPI is available at '/openapi' and Swagger UI is available at '/swagger-ui'.")
            .buildTime(true)
            .defaultValue(Boolean.FALSE)
            .build();
}
