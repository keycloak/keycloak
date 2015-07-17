package org.keycloak.authentication;

import org.keycloak.provider.Provider;

import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RequiredActionProvider extends Provider {
    void evaluateTriggers(RequiredActionContext context);
    Response invokeRequiredAction(RequiredActionContext context);
    Object jaxrsService(RequiredActionContext context);
    String getProviderId();
}
