package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.config.OpenApiOptions;
import org.keycloak.config.SwaggerOptions;

import static org.keycloak.quarkus.runtime.configuration.Configuration.isTrue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;


final class OpenApiPropertyMappers {

  public static final String OPENAPI_ENABLED_MSG = "OpenApi is enabled";

  private OpenApiPropertyMappers() {
  }

  public static PropertyMapper<?>[] getOpenApiPropertyMappers() {
    return new PropertyMapper[]{
        fromOption(OpenApiOptions.OPENAPI_ENABLED)
            .to("quarkus.smallrye-openapi.enable")
            .build(),
        fromOption(OpenApiOptions.OPENAPI_PATH)
            .to("quarkus.smallrye-openapi.path")
            .build(),
        fromOption(OpenApiOptions.OPENAPI_STORE_SCHEMA_DIR)
            .to("quarkus.smallrye-openapi.store-schema-directory")
            .build(),
    };
  }

  public static boolean openApiEnabled() {
    return isTrue(OpenApiOptions.OPENAPI_ENABLED);
  }
}
