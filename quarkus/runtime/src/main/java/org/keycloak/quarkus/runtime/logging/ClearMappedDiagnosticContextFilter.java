package org.keycloak.quarkus.runtime.logging;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import org.keycloak.logging.MappedDiagnosticContextUtil;
import org.slf4j.MDC;

import java.io.IOException;

/**
 * Response filter that clears custom properties from MDC.
 *
 * @author <a href="mailto:b.eicki@gmx.net">Björn Eickvonder</a>
 */
@Provider
@Priority(0)
public class ClearMappedDiagnosticContextFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        MappedDiagnosticContextUtil.clearMdc();
    }
}
