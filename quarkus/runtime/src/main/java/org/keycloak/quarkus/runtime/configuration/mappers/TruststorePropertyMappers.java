package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.List;

import org.keycloak.config.TruststoreOptions;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public class TruststorePropertyMappers implements PropertyMapperGrouping {

    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        return List.of(
                fromOption(TruststoreOptions.TRUSTSTORE_PATHS)
                        .paramLabel(TruststoreOptions.TRUSTSTORE_PATHS.getKey())
                        .build(),
                fromOption(TruststoreOptions.HOSTNAME_VERIFICATION_POLICY)
                        .paramLabel(TruststoreOptions.HOSTNAME_VERIFICATION_POLICY.getKey())
                        .to("kc.spi-truststore--file--hostname-verification-policy")
                        .build()
        );
    }

}
