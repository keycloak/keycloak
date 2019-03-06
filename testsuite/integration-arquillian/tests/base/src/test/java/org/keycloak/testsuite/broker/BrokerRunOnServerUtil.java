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
package org.keycloak.testsuite.broker;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.testsuite.runonserver.RunOnServer;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
final class BrokerRunOnServerUtil {

    static RunOnServer configurePostBrokerLoginWithOTP(String idpAlias) {
        return (session) -> {
            RealmModel realm = session.getContext().getRealm();

            // Add post-broker flow with OTP authenticator to the realm
            AuthenticationFlowModel postBrokerFlow = new AuthenticationFlowModel();
            postBrokerFlow.setAlias("post-broker");
            postBrokerFlow.setDescription("post-broker flow with OTP");
            postBrokerFlow.setProviderId("basic-flow");
            postBrokerFlow.setTopLevel(true);
            postBrokerFlow.setBuiltIn(false);
            postBrokerFlow = realm.addAuthenticationFlow(postBrokerFlow);

            AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
            execution.setParentFlow(postBrokerFlow.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator("auth-otp-form");
            execution.setPriority(20);
            execution.setAuthenticatorFlow(false);
            realm.addAuthenticatorExecution(execution);

            IdentityProviderModel idp = realm.getIdentityProviderByAlias(idpAlias);
            idp.setPostBrokerLoginFlowId(postBrokerFlow.getId());
            realm.updateIdentityProvider(idp);
        };
    }

    static RunOnServer disablePostBrokerLoginFlow(String idpAlias) {
        return session -> {
            RealmModel realm = session.getContext().getRealm();

            IdentityProviderModel idp = realm.getIdentityProviderByAlias(idpAlias);
            idp.setPostBrokerLoginFlowId(null);
            realm.updateIdentityProvider(idp);
        };
    }

    static RunOnServer grantReadTokenRole(String username) {
        return session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel brokerClient = realm.getClientByClientId(Constants.BROKER_SERVICE_CLIENT_ID);
            RoleModel readTokenRole = brokerClient.getRole(Constants.READ_TOKEN_ROLE);
            UserModel user = session.users().getUserByUsername(username, realm);
            user.grantRole(readTokenRole);
        };
    }

    static RunOnServer revokeReadTokenRole(String username) {
        return session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel brokerClient = realm.getClientByClientId(Constants.BROKER_SERVICE_CLIENT_ID);
            RoleModel readTokenRole = brokerClient.getRole(Constants.READ_TOKEN_ROLE);
            UserModel user = session.users().getUserByUsername(username, realm);
            user.deleteRoleMapping(readTokenRole);
        };
    }

    static RunOnServer configureAutoLinkFlow(String idpAlias) {
        return (session -> {
            RealmModel appRealm = session.getContext().getRealm();
            AuthenticationFlowModel newFlow = new AuthenticationFlowModel();
            newFlow.setAlias("AutoLink");
            newFlow.setDescription("AutoLink");
            newFlow.setProviderId("basic-flow");
            newFlow.setBuiltIn(false);
            newFlow.setTopLevel(true);
            newFlow = appRealm.addAuthenticationFlow(newFlow);

            AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            execution.setAuthenticatorFlow(false);
            execution.setAuthenticator("idp-create-user-if-unique");
            execution.setPriority(1);
            execution.setParentFlow(newFlow.getId());
            execution = appRealm.addAuthenticatorExecution(execution);

            AuthenticationExecutionModel execution2 = new AuthenticationExecutionModel();
            execution2.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            execution2.setAuthenticatorFlow(false);
            execution2.setAuthenticator("idp-auto-link");
            execution2.setPriority(2);
            execution2.setParentFlow(newFlow.getId());
            execution2 = appRealm.addAuthenticatorExecution(execution2);

            IdentityProviderModel idp = appRealm.getIdentityProviderByAlias(idpAlias);
            idp.setFirstBrokerLoginFlowId(newFlow.getId());
            appRealm.updateIdentityProvider(idp);
        });
    }

    static RunOnServer assertHardCodedSessionNote() {
        return (session) -> {
            RealmModel realm = session.realms().getRealmByName("consumer");
            UserModel user = session.users().getUserByUsername("testuser", realm);
            List<UserSessionModel> userSessions = session.sessions().getUserSessions(realm, user);
            UserSessionModel sessions = userSessions.get(0);
            assertEquals("sessionvalue", sessions.getNote("user-session-attr"));
        };
    }

    static RunOnServer removeBrokerExpiredSessions() {
        return (RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            session.sessions().removeExpired(realm);
            session.authenticationSessions().removeExpired(realm);
        };
    }
}
