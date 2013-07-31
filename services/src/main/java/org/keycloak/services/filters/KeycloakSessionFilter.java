package org.keycloak.services.filters;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.KeycloakSessionFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@PreMatching
public class KeycloakSessionFilter implements ContainerRequestFilter, ContainerResponseFilter {
    protected static final Logger logger = Logger.getLogger(KeycloakSessionFilter.class);
    protected KeycloakSessionFactory factory;

    public KeycloakSessionFilter(KeycloakSessionFactory factory) {
        this.factory = factory;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        KeycloakSession ctx = factory.createSession();
        requestContext.setProperty(KeycloakSession.class.getName(), ctx);
        ResteasyProviderFactory.pushContext(KeycloakSession.class, ctx);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        KeycloakSession ctx = (KeycloakSession)requestContext.getProperty(KeycloakSession.class.getName());
        if (ctx != null) ctx.close();
    }
}
