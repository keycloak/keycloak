package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.config.OpenApiOptions;

import static org.keycloak.quarkus.runtime.configuration.Configuration.isTrue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public final class OpenApiPropertyMappers {

  private OpenApiPropertyMappers() {
  }

  public static PropertyMapper<?>[] getOpenApiPropertyMappers() {
    return new PropertyMapper[]{
        fromOption(OpenApiOptions.OPENAPI_ENABLED)
            .to("quarkus.smallrye-openapi.enable")
            .build(),
        fromOption(OpenApiOptions.OPENAPI_UI_ENABLED)
            .isEnabled(OpenApiPropertyMappers::isUiEnabled, "OpenAPI Endpoint is enabled")
            .to("quarkus.swagger-ui.enable")
            .build(),
    };
  }

  private static boolean isUiEnabled() {
    return isTrue(OpenApiOptions.OPENAPI_ENABLED);
  }
}
