package org.keycloak.services.filters;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.KeycloakSessionFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@PreMatching
public class KeycloakSessionCreateFilter implements ContainerRequestFilter {
    protected static final Logger logger = Logger.getLogger(KeycloakSessionCreateFilter.class);
    protected KeycloakSessionFactory factory;

    public KeycloakSessionCreateFilter(KeycloakSessionFactory factory) {
        this.factory = factory;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        KeycloakSession ctx = factory.createSession();
        ResteasyProviderFactory.pushContext(KeycloakSession.class, ctx);
    }

}
