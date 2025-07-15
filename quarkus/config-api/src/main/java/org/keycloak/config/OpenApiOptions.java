package org.keycloak.config;

public class OpenApiOptions {

  public static final Option<Boolean> OPENAPI_ENABLED = new OptionBuilder<>("openapi-enabled", Boolean.class)
      .category(OptionCategory.OPENAPI_SWAGGER)
      .description("If the server should expose OpenAPI Endpoint. If enabled, OpenAPI is available at '/openapi'.")
      .buildTime(true)
      .defaultValue(Boolean.FALSE)
      .build();

  public static final Option<String> OPENAPI_PATH = new OptionBuilder<>("openapi-path", String.class)
      .category(OptionCategory.OPENAPI_SWAGGER)
      .description("OpenAPI path, default is '/openapi'.")
      .buildTime(true)
      .defaultValue("/openapi")
      .build();

  public static final Option<String> OPENAPI_STORE_SCHEMA_DIR = new OptionBuilder<>("openapi-store-schema-dir", String.class)
      .category(OptionCategory.OPENAPI_SWAGGER)
      .description("OpenAPI store schema directory, default is '${openapi.schema.target}'.")
      .buildTime(true)
      .defaultValue("${openapi.schema.target}")
      .build();
}
