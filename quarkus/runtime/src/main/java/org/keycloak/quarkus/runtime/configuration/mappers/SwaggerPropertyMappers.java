package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.config.SwaggerOptions;

import static org.keycloak.quarkus.runtime.configuration.Configuration.isTrue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;


final class SwaggerPropertyMappers {

  public static final String SWAGGER_ENABLED_MSG = "Swagger UI is enabled";

  private SwaggerPropertyMappers() {
  }

  public static PropertyMapper<?>[] getSwaggerPropertyMappers() {
    return new PropertyMapper[]{
        fromOption(SwaggerOptions.SWAGGER_ENABLED)
            .to("quarkus.swagger-ui.enable")
            .build(),
        fromOption(SwaggerOptions.SWAGGER_PATH)
            .to("quarkus.swagger-ui.path")
            .build(),
        fromOption(SwaggerOptions.SWAGGER_ALWAYS_INCLUDE)
            .to("quarkus.swagger-ui.always-include")
            .build(),
    };
  }

  public static boolean swaggerEnabled() {
    return isTrue(SwaggerOptions.SWAGGER_ENABLED);
  }
}
