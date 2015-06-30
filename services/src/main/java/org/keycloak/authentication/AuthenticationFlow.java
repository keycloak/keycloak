package org.keycloak.authentication;

import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface AuthenticationFlow {
    Response processAction(String actionExecution);
    Response processFlow();
}
