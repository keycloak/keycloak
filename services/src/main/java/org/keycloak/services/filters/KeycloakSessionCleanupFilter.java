package org.keycloak.services.filters;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.services.models.KeycloakSession;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakSessionCleanupFilter implements ContainerResponseFilter {
    protected static final Logger logger = Logger.getLogger(KeycloakSessionCleanupFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        KeycloakSession ctx = ResteasyProviderFactory.getContextData(KeycloakSession.class);
        if (ctx != null) ctx.close();
    }
}
