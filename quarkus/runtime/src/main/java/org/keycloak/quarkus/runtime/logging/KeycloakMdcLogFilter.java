package org.keycloak.quarkus.runtime.logging;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import org.keycloak.logging.MdcDefinitionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.KeycloakSessionUtil;

import java.io.IOException;
import java.util.Map;

/**
 * Request and response filter that sets and clears MDC values for the current request.
 * The {@link MdcDefinitionProvider} defines which values are actually set.
 *
 * @author <a href="mailto:b.eicki@gmx.net">Björn Eickvonder</a>
 */
@Provider
@Priority(9999)
public class KeycloakMdcLogFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(KeycloakMdcLogFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
        LOG.tracef("Request %s %s has session %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getPath(),
                session);
        if (session != null && session.getContext() != null) {
            MdcDefinitionProvider provider = session.getProvider(MdcDefinitionProvider.class);
            for (Map.Entry<String, String> entry : provider.getMdcValues(session.getContext()).entrySet()) {
                MDC.put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
        MdcDefinitionProvider provider = session.getProvider(MdcDefinitionProvider.class);
        for (String key : provider.getMdcKeys()) {
            MDC.remove(key);
        }
    }
}
