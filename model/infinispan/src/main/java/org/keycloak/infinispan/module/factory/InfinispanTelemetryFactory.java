package org.keycloak.infinispan.module.factory;

import io.opentelemetry.api.OpenTelemetry;
import jakarta.enterprise.inject.spi.CDI;
import org.infinispan.factories.AbstractComponentFactory;
import org.infinispan.factories.AutoInstantiableFactory;
import org.infinispan.factories.annotations.DefaultFactoryFor;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.telemetry.InfinispanTelemetry;
import org.infinispan.telemetry.impl.DisabledInfinispanTelemetry;

@Scope(Scopes.GLOBAL)
@DefaultFactoryFor(classes = InfinispanTelemetry.class)
public class InfinispanTelemetryFactory extends AbstractComponentFactory implements AutoInstantiableFactory {

    @Override
    public Object construct(String componentName) {
        OpenTelemetry openTelemetry = CDI.current().select(OpenTelemetry.class).get();
        if (openTelemetry == null) {
            return new DisabledInfinispanTelemetry();
        } else {
            return new OpenTelemetryService(openTelemetry);
        }
    }
}
