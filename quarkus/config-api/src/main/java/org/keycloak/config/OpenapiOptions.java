package org.keycloak.config;

public class OpenapiOptions {

    public static final Option OPENAPI_ENABLE = new OptionBuilder<>("openapi-enable", Boolean.class)
            .category(OptionCategory.OPENAPI)
            .description("Enable the openapi endpoint.")
            .defaultValue(false)
            .buildTime(true)
            .build();

    public static final Option OPENAPI_SWAGGER_UI_ENABLE = new OptionBuilder<>("openapi-swagger-ui-enable", Boolean.class)
            .category(OptionCategory.OPENAPI)
            .description("Enable Swagger UI endpoint.")
            .defaultValue(false)
            .buildTime(true)
            .build();
}
