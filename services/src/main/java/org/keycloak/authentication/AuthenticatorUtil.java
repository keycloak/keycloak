package org.keycloak.authentication;

import org.keycloak.authentication.authenticators.LoginFormPasswordAuthenticatorFactory;
import org.keycloak.authentication.authenticators.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.SpnegoAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.CredentialRepresentation;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticatorUtil {

    public static AuthenticationExecutionModel findExecutionByAuthenticator(RealmModel realm, String flowId, String authProviderId) {
        for (AuthenticationExecutionModel model : realm.getAuthenticationExecutions(flowId)) {
            if (model.isAutheticatorFlow()) {
                AuthenticationExecutionModel recurse = findExecutionByAuthenticator(realm, model.getAuthenticator(), authProviderId);
                if (recurse != null) return recurse;

            }
            AuthenticatorModel authenticator = realm.getAuthenticatorById(model.getAuthenticator());
            if (authenticator.getProviderId().equals(authProviderId)) {
                return model;
            }
        }
        return null;
    }

    public static boolean isEnabled(RealmModel realm, String flowId, String authProviderId) {
        AuthenticationExecutionModel execution = findExecutionByAuthenticator(realm, flowId, authProviderId);
        if (execution == null) {
            return false;
        }
        return execution.isEnabled();
    }
    public static boolean isRequired(RealmModel realm, String flowId, String authProviderId) {
        AuthenticationExecutionModel execution = findExecutionByAuthenticator(realm, flowId, authProviderId);
        if (execution == null) {
            return false;
        }
        return execution.isRequired();
    }
}
