package org.keycloak.utils;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ClientAuthenticator;
import org.keycloak.authentication.ClientAuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * used to set an execution a state based on type.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CredentialHelper {

    public static void setRequiredCredential(KeycloakSession session, String type, RealmModel realm) {
        AuthenticationExecutionModel.Requirement requirement = AuthenticationExecutionModel.Requirement.REQUIRED;
        authenticationRequirement(session, realm, type, requirement);
    }

    public static void setAlternativeCredential(KeycloakSession session, String type, RealmModel realm) {
        AuthenticationExecutionModel.Requirement requirement = AuthenticationExecutionModel.Requirement.ALTERNATIVE;
        authenticationRequirement(session, realm, type, requirement);
    }

    public static void authenticationRequirement(KeycloakSession session, RealmModel realm, String type, AuthenticationExecutionModel.Requirement requirement) {
        for (AuthenticationFlowModel flow : realm.getAuthenticationFlows()) {
            for (AuthenticationExecutionModel execution : realm.getAuthenticationExecutions(flow.getId())) {
                String providerId = execution.getAuthenticator();
                ConfigurableAuthenticatorFactory factory = getConfigurableAuthenticatorFactory(session, providerId);
                if (factory == null) continue;
                if (type.equals(factory.getReferenceCategory())) {
                    execution.setRequirement(requirement);
                    realm.updateAuthenticatorExecution(execution);
                }
            }
        }
    }

     public static ConfigurableAuthenticatorFactory getConfigurableAuthenticatorFactory(KeycloakSession session, String providerId) {
         ConfigurableAuthenticatorFactory factory = (AuthenticatorFactory)session.getKeycloakSessionFactory().getProviderFactory(Authenticator.class, providerId);
         if (factory == null) {
             factory = (FormActionFactory)session.getKeycloakSessionFactory().getProviderFactory(FormAction.class, providerId);
         }
         if (factory == null) {
             factory = (ClientAuthenticatorFactory)session.getKeycloakSessionFactory().getProviderFactory(ClientAuthenticator.class, providerId);
         }
         return factory;
    }
}
