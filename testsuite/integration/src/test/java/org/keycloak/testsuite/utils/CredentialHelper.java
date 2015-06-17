package org.keycloak.testsuite.utils;

import org.keycloak.authentication.authenticators.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.SpnegoAuthenticatorFactory;
import org.keycloak.authentication.authenticators.UsernamePasswordFormFactory;
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
public class CredentialHelper {

    public static void setRequiredCredential(String type, RealmModel realm) {
        AuthenticationExecutionModel.Requirement requirement = AuthenticationExecutionModel.Requirement.REQUIRED;
        setCredentialRequirement(type, realm, requirement);
    }

    public static void setAlternativeCredential(String type, RealmModel realm) {
        AuthenticationExecutionModel.Requirement requirement = AuthenticationExecutionModel.Requirement.ALTERNATIVE;
        setCredentialRequirement(type, realm, requirement);
    }

    public static void setCredentialRequirement(String type, RealmModel realm, AuthenticationExecutionModel.Requirement requirement) {
        if (type.equals(CredentialRepresentation.TOTP)) {
            String providerId = OTPFormAuthenticatorFactory.PROVIDER_ID;
            String flowAlias = DefaultAuthenticationFlows.FORMS_FLOW;
            authenticationRequirement(realm, providerId, flowAlias, requirement);
        } else if (type.equals(CredentialRepresentation.KERBEROS)) {
            String providerId = SpnegoAuthenticatorFactory.PROVIDER_ID;
            String flowAlias = DefaultAuthenticationFlows.BROWSER_FLOW;
            authenticationRequirement(realm, providerId, flowAlias, requirement);
        } else if (type.equals(CredentialRepresentation.PASSWORD)) {
            String providerId = UsernamePasswordFormFactory.PROVIDER_ID;
            String flowAlias = DefaultAuthenticationFlows.FORMS_FLOW;
            authenticationRequirement(realm, providerId, flowAlias, requirement);
        }
    }

    public static AuthenticationExecutionModel.Requirement getRequirement(RealmModel realm, String authenticatorProviderId, String flowAlias) {
        AuthenticatorModel authenticator = findAuthenticatorByProviderId(realm, authenticatorProviderId);
        AuthenticationFlowModel flow =  findAuthenticatorFlowByAlias(realm, flowAlias);
        AuthenticationExecutionModel execution = findExecutionByAuthenticator(realm, flow.getId(), authenticator.getId());
        return execution.getRequirement();

    }

    public static void alternativeAuthentication(RealmModel realm, String authenticatorProviderId, String flowAlias) {
        AuthenticationExecutionModel.Requirement requirement = AuthenticationExecutionModel.Requirement.ALTERNATIVE;
        authenticationRequirement(realm, authenticatorProviderId, flowAlias, requirement);
    }

    public static void authenticationRequirement(RealmModel realm, String authenticatorProviderId, String flowAlias, AuthenticationExecutionModel.Requirement requirement) {
        AuthenticatorModel authenticator = findAuthenticatorByProviderId(realm, authenticatorProviderId);
        AuthenticationFlowModel flow =  findAuthenticatorFlowByAlias(realm, flowAlias);
        AuthenticationExecutionModel execution = findExecutionByAuthenticator(realm, flow.getId(), authenticator.getId());
        execution.setRequirement(requirement);
        realm.updateAuthenticatorExecution(execution);
    }

    public static AuthenticatorModel findAuthenticatorByProviderId(RealmModel realm, String providerId) {
        for (AuthenticatorModel model : realm.getAuthenticators()) {
            if (model.getProviderId().equals(providerId)) {
                return model;
            }
        }
        return null;
    }
    public static AuthenticationFlowModel findAuthenticatorFlowByAlias(RealmModel realm, String alias) {
        for (AuthenticationFlowModel model : realm.getAuthenticationFlows()) {
            if (model.getAlias().equals(alias)) {
                return model;
            }
        }
        return null;
    }
    public static AuthenticationExecutionModel findExecutionByAuthenticator(RealmModel realm, String flowId, String authId) {
        for (AuthenticationExecutionModel model : realm.getAuthenticationExecutions(flowId)) {
            if (model.getAuthenticator().equals(authId)) {
                return model;
            }
        }
        return null;

    }
}
