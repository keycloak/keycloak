package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.quarkus.runtime.cli.Picocli;

public interface PropertyMapperGrouping {

    PropertyMapper<?>[] getPropertyMappers();

    default void validateConfig(Picocli picocli) {

    }

}
