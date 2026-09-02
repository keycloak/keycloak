package org.keycloak.ssf.event;

/**
 * Default {@link SsfEventProvider} implementation that simply exposes the
 * pre-built {@link SsfEventRegistry}.
 */
public class DefaultSsfEventProvider implements SsfEventProvider {

    private final SsfEventRegistry registry;

    public DefaultSsfEventProvider(SsfEventRegistry registry) {
        this.registry = registry;
    }

    @Override
    public SsfEventRegistry getRegistry() {
        return registry;
    }
}
