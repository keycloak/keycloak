package org.keycloak.quarkus.runtime.integration.jaxrs;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import org.keycloak.services.util.Projections;

@Provider
@PreMatching
public class ProjectionsPreFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String projectionsParam = requestContext.getUriInfo().getQueryParameters().getFirst(Projections.PARAM);
        if (projectionsParam != null) {
            Projections.setCurrent(requestContext, projectionsParam);
        }
    }
}
