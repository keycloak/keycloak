package org.keycloak.jaxrs;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public interface JaxrsBearerTokenFilter extends ContainerRequestFilter {
}
