package org.keycloak.config;

public class SwaggerOptions {

  public static final Option<Boolean> SWAGGER_ENABLED = new OptionBuilder<>("swagger-enabled", Boolean.class)
      .category(OptionCategory.OPENAPI_SWAGGER)
      .description("If the server should expose Swagger-UI Endpoint. If enabled, Swagger UI is available at '/swagger-ui'.")
      .buildTime(true)
      .defaultValue(Boolean.FALSE)
      .build();

  public static final Option<String> SWAGGER_PATH = new OptionBuilder<>("swagger-path", String.class)
      .category(OptionCategory.OPENAPI_SWAGGER)
      .description("Swagger UI path, default is '/swagger-ui'.")
      .buildTime(true)
      .defaultValue("/swagger-ui")
      .build();

  public static final Option<Boolean> SWAGGER_ALWAYS_INCLUDE = new OptionBuilder<>("swagger-always-include", Boolean.class)
      .category(OptionCategory.OPENAPI_SWAGGER)
      .description("If Swagger UI should always be included, even if the server is not executed in dev mode.")
      .buildTime(true)
      .defaultValue(true)
      .build();
}
