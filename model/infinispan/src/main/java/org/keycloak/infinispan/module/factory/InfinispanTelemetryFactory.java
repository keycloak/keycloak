package org.keycloak.infinispan.module.factory;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import io.opentelemetry.api.OpenTelemetry;
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
        CDI<Object> current;
        try {
            current = CDI.current();
        } catch (IllegalStateException e) {
            // No CDI context, assume tracing is not available
            return new DisabledInfinispanTelemetry();
        }
        Instance<OpenTelemetry> selector = current.select(OpenTelemetry.class);
        if (!selector.isResolvable()) {
            return new DisabledInfinispanTelemetry();
        } else {
            return new OpenTelemetryService(selector.get());
        }
    }
}
