package org.keycloak.authentication;

import org.keycloak.models.AuthenticationExecutionModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface FormActionContext extends AuthenticatorContext {
    FormAuthenticator getFormAuthenticator();
    AuthenticationExecutionModel getFormExecution();
}
