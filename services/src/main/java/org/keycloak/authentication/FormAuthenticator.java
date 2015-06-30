package org.keycloak.authentication;

import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.Provider;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface FormAuthenticator extends Provider {
    void authenticate(AuthenticatorContext context);
    Response createChallenge(FormActionContext context, MultivaluedMap<String, String> formData, List<FormMessage> errorMessages);
}
