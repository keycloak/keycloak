/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.utils;

import org.jboss.logging.Logger;
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

    private static final Logger logger = Logger.getLogger(CredentialHelper.class);

    public static void setRequiredCredential(KeycloakSession session, String type, RealmModel realm) {
        AuthenticationExecutionModel.Requirement requirement = AuthenticationExecutionModel.Requirement.REQUIRED;
        setOrReplaceAuthenticationRequirement(session, realm, type, requirement, null);
    }

    public static void setAlternativeCredential(KeycloakSession session, String type, RealmModel realm) {
        AuthenticationExecutionModel.Requirement requirement = AuthenticationExecutionModel.Requirement.ALTERNATIVE;
        setOrReplaceAuthenticationRequirement(session, realm, type, requirement, null);
    }

    public static void setOrReplaceAuthenticationRequirement(KeycloakSession session, RealmModel realm, String type, AuthenticationExecutionModel.Requirement requirement, AuthenticationExecutionModel.Requirement currentRequirement) {
        for (AuthenticationFlowModel flow : realm.getAuthenticationFlows()) {
            for (AuthenticationExecutionModel execution : realm.getAuthenticationExecutions(flow.getId())) {
                String providerId = execution.getAuthenticator();
                ConfigurableAuthenticatorFactory factory = getConfigurableAuthenticatorFactory(session, providerId);
                if (factory == null) continue;
                if (type.equals(factory.getReferenceCategory())) {
                    if (currentRequirement == null || currentRequirement.equals(execution.getRequirement())) {
                        execution.setRequirement(requirement);
                        realm.updateAuthenticatorExecution(execution);
                        logger.debugf("Authenticator execution '%s' switched to '%s'", execution.getAuthenticator(), requirement.toString());
                    } else {
                        logger.debugf("Skip switch authenticator execution '%s' to '%s' as it's in state %s", execution.getAuthenticator(), requirement.toString(), execution.getRequirement());
                    }
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
