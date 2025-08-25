package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.config.HealthOptions;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;


final class HealthPropertyMappers implements PropertyMapperGrouping {


    @Override
    public PropertyMapper<?>[] getPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(HealthOptions.HEALTH_ENABLED)
                        // no need to map to a quarkus option, this option exists to
                        // to control artifact / extension inclusion. Quarkus will default to enabled
                        .build()
        };
    }

}
