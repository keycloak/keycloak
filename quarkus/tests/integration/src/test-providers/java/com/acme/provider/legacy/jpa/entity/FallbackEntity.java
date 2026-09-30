package com.acme.provider.legacy.jpa.entity;

import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;

import org.keycloak.provider.Provider;

/**
 * Exists to assert that an unmapped {@code @Entity} in a provider JAR (without persistence.xml or orm.xml)
 * falls back to the default persistence unit, together with its mapped superclass, embeddable, converter and the
 * interfaces it implements from a JAR that is not indexed (like {@link Provider} from keycloak-server-spi).
 */
@Entity
public class FallbackEntity extends FallbackBase implements Provider {

    @Embedded
    private FallbackEmbeddable embeddable;

    @Convert(converter = FallbackConverter.class)
    private Boolean flag;

    @Override
    public void close() {
    }
}
