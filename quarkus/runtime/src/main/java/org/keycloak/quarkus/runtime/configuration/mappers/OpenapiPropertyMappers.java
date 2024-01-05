package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.config.OpenapiOptions;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class OpenapiPropertyMappers {

    private OpenapiPropertyMappers(){}

    public static PropertyMapper[] getOpenapiPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(OpenapiOptions.OPENAPI_ENABLE)
                        .to("quarkus.smallrye-openapi.enable")
                        .paramLabel("openapi")
                        .build(),
                fromOption(OpenapiOptions.OPENAPI_SWAGGER_UI_ENABLE)
                        .to("quarkus.swagger-ui.enable")
                        .paramLabel("swagger-ui")
                        .build()
        };
    }
}

