package org.keycloak.testsuite.arquillian.jetty;

import org.jboss.arquillian.container.jetty.embedded_9.ArquillianAppProvider;
import org.jboss.shrinkwrap.api.Archive;

class JettyAppProvider extends ArquillianAppProvider {

    public JettyAppProvider(JettyAppServerConfiguration config) {
        super(config);
    }

    protected KeycloakAdapterApp createApp(final Archive<?> archive) {
        return new KeycloakAdapterApp(super.createApp(archive), archive);
    }
}
