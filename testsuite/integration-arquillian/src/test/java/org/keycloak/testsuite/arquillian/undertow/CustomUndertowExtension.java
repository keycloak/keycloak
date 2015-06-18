package org.keycloak.testsuite.arquillian.undertow;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.core.spi.LoadableExtension;

public class CustomUndertowExtension implements LoadableExtension {

    public void register(ExtensionBuilder builder) {
        builder.service(DeployableContainer.class, EmbeddedUndertowContainer.class);
    }

}
