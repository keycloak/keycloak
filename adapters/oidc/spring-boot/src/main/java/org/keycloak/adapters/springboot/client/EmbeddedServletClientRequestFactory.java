package org.keycloak.adapters.springboot.client;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.client.KeycloakClientRequestFactory;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.Principal;

/**
 * Factory for {@link ClientHttpRequest} objects created for server to server secured
 * communication using OAuth2 bearer tokens issued by Keycloak.
 *
 * @author <a href="mailto:jmcshan1@gmail.com">James McShane</a>
 * @version $Revision: 1 $
 */
public class EmbeddedServletClientRequestFactory extends KeycloakClientRequestFactory {

    public EmbeddedServletClientRequestFactory() {
        super();
    }

    /**
     * Returns the {@link KeycloakSecurityContext} from the Spring {@link ServletRequestAttributes}'s {@link Principal}.
     *
     * The principal must support retrieval of the KeycloakSecurityContext, so at this point, only {@link KeycloakPrincipal}
     * values are supported
     *
     * @return the current <code>KeycloakSecurityContext</code>
     */
    protected KeycloakSecurityContext getKeycloakSecurityContext() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        Principal principal = attributes.getRequest().getUserPrincipal();
        if (principal == null) {
            throw new IllegalStateException("Cannot set authorization header because there is no authenticated principal");
        }
        if (!(principal instanceof KeycloakPrincipal)) {
            throw new IllegalStateException(
                    String.format(
                            "Cannot set authorization header because the principal type %s does not provide the KeycloakSecurityContext",
                            principal.getClass()));
        }
        return ((KeycloakPrincipal) principal).getKeycloakSecurityContext();
    }
}
