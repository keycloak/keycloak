package org.keycloak.testsuite.arquillian.undertow;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.keycloak.testsuite.arquillian.undertow.lb.SimpleUndertowLoadBalancerContainer;

/**
 *
 * @author tkyjovsk
 */
public class KeycloakOnUndertowArquillianExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(DeployableContainer.class, KeycloakOnUndertow.class);
        builder.service(DeployableContainer.class, SimpleUndertowLoadBalancerContainer.class);
    }

}
