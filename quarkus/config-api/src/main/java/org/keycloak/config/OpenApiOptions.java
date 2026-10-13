package org.keycloak.config;

public class OpenApiOptions {

  public static final Option<Boolean> OPENAPI_ENABLED = new OptionBuilder<>("openapi-enabled", Boolean.class)
      .category(OptionCategory.OPENAPI)
      .description("If the server should expose OpenAPI Endpoint. If enabled, OpenAPI is available at '/openapi'.")
      .buildTime(true)
      .defaultValue(Boolean.FALSE)
      .build();
  public static final Option<Boolean> OPENAPI_UI_ENABLED = new OptionBuilder<>("openapi-ui-enabled", Boolean.class)
      .category(OptionCategory.OPENAPI)
      .description("If the server should expose OpenApi-UI Endpoint. If enabled, OpenAPI UI is available at '/openapi/ui'.")
      .buildTime(true)
      .defaultValue(Boolean.FALSE)
      .build();
}
