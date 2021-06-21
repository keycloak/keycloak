/*
 *
 *  * Copyright 2021  Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.testsuite.user.profile;

import static org.keycloak.userprofile.DeclarativeUserProfileProvider.REALM_USER_PROFILE_ENABLED;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.userprofile.DeclarativeUserProfileProvider;
import org.keycloak.userprofile.UserProfileProvider;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractUserProfileTest extends AbstractTestRealmKeycloakTest {

    protected static void configureAuthenticationSession(KeycloakSession session) {
        Set<String> scopes = new HashSet<>();

        scopes.add("customer");

        configureAuthenticationSession(session, "client-a", scopes);
    }

    protected static void configureAuthenticationSession(KeycloakSession session, String clientId, Set<String> requestedScopes) {
        RealmModel realm = session.getContext().getRealm();

        session.getContext().setAuthenticationSession(createAuthenticationSession(realm.getClientByClientId(clientId), requestedScopes));
    }

    protected static DeclarativeUserProfileProvider getDynamicUserProfileProvider(KeycloakSession session) {
        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);

        provider.setConfiguration(null);

        return (DeclarativeUserProfileProvider) provider;
    }

    protected static AuthenticationSessionModel createAuthenticationSession(ClientModel client, Set<String> scopes) {
        return new AuthenticationSessionModel() {
            @Override
            public String getTabId() {
                return null;
            }

            @Override
            public RootAuthenticationSessionModel getParentSession() {
                return null;
            }

            @Override
            public Map<String, ExecutionStatus> getExecutionStatus() {
                return null;
            }

            @Override
            public void setExecutionStatus(String authenticator, ExecutionStatus status) {

            }

            @Override
            public void clearExecutionStatus() {

            }

            @Override
            public UserModel getAuthenticatedUser() {
                return null;
            }

            @Override
            public void setAuthenticatedUser(UserModel user) {

            }

            @Override
            public Set<String> getRequiredActions() {
                return null;
            }

            @Override
            public void addRequiredAction(String action) {

            }

            @Override
            public void removeRequiredAction(String action) {

            }

            @Override
            public void addRequiredAction(UserModel.RequiredAction action) {

            }

            @Override
            public void removeRequiredAction(UserModel.RequiredAction action) {

            }

            @Override
            public void setUserSessionNote(String name, String value) {

            }

            @Override
            public Map<String, String> getUserSessionNotes() {
                return null;
            }

            @Override
            public void clearUserSessionNotes() {

            }

            @Override
            public String getAuthNote(String name) {
                return null;
            }

            @Override
            public void setAuthNote(String name, String value) {

            }

            @Override
            public void removeAuthNote(String name) {

            }

            @Override
            public void clearAuthNotes() {

            }

            @Override
            public String getClientNote(String name) {
                return null;
            }

            @Override
            public void setClientNote(String name, String value) {

            }

            @Override
            public void removeClientNote(String name) {

            }

            @Override
            public Map<String, String> getClientNotes() {
                return null;
            }

            @Override
            public void clearClientNotes() {

            }

            @Override
            public Set<String> getClientScopes() {
                return scopes;
            }

            @Override
            public void setClientScopes(Set<String> clientScopes) {

            }

            @Override
            public String getRedirectUri() {
                return null;
            }

            @Override
            public void setRedirectUri(String uri) {

            }

            @Override
            public RealmModel getRealm() {
                return null;
            }

            @Override
            public ClientModel getClient() {
                return client;
            }

            @Override
            public String getAction() {
                return null;
            }

            @Override
            public void setAction(String action) {

            }

            @Override
            public String getProtocol() {
                return null;
            }

            @Override
            public void setProtocol(String method) {

            }
        };
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        if (testRealm.getAttributes() == null) {
            testRealm.setAttributes(new HashMap<>());
        }
        testRealm.getAttributes().put(REALM_USER_PROFILE_ENABLED, Boolean.TRUE.toString());
    }
}
