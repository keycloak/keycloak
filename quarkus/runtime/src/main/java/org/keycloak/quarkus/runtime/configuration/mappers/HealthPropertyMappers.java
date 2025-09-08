package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.config.HealthOptions;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

import java.util.List;


final class HealthPropertyMappers implements PropertyMapperGrouping {


    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        return List.of(
                fromOption(HealthOptions.HEALTH_ENABLED)
                        // no need to map to a quarkus option, this option exists to
                        // to control artifact / extension inclusion. Quarkus will default to enabled
                        .build()
        );
    }

}
