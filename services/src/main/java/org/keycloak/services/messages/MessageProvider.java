package org.keycloak.services.messages;

import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:leonardo.zanivan@gmail.com">Leonardo Zanivan</a>
 */
public interface MessageProvider {

    String getMessage(KeycloakSession session, String messageKey, Object... parameters);
    
}
