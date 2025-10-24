package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.common.Profile;
import org.keycloak.config.OpenApiOptions;

import java.util.List;

import static org.keycloak.quarkus.runtime.configuration.Configuration.isTrue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public final class OpenApiPropertyMappers implements PropertyMapperGrouping {

  @Override
  public List<? extends PropertyMapper<?>> getPropertyMappers() {
    return List.of(
        fromOption(OpenApiOptions.OPENAPI_ENABLED)
            .isEnabled(OpenApiPropertyMappers::isClientApiEnabled, "Client Admin API v2 Feature is enabled")
            .to("quarkus.smallrye-openapi.enable")
            .build(),
        fromOption(OpenApiOptions.OPENAPI_UI_ENABLED)
            .isEnabled(OpenApiPropertyMappers::isOpenApiEnabled, "OpenAPI Endpoint is enabled")
            .to("quarkus.swagger-ui.enable")
            .build()
    );
  }

  private static boolean isOpenApiEnabled() {
    return isTrue(OpenApiOptions.OPENAPI_ENABLED);
  }

  private static boolean isClientApiEnabled() {
    return Profile.isFeatureEnabled(Profile.Feature.CLIENT_ADMIN_API_V2);
  }
}
