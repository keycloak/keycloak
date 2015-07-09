package org.keycloak.authentication;

import org.keycloak.login.LoginFormsProvider;
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
    Response render(FormContext context, LoginFormsProvider form);
}
