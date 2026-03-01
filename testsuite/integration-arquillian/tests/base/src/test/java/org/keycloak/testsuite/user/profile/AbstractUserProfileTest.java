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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.keycloak.OAuth2Constants;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.config.UPConfigUtils;
import org.keycloak.util.JsonSerialization;

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

        ClientModel client = realm.getClientByClientId(clientId);
        session.getContext().setAuthenticationSession(createAuthenticationSession(client, requestedScopes));
        session.getContext().setClient(client);
    }

    protected static Optional<ComponentModel> setAndGetDefaultConfiguration(KeycloakSession session) {
        setDefaultConfiguration(session);
        return getComponentModel(session);
    }

    protected static Optional<ComponentModel> getComponentModel(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        return realm.getComponentsStream(realm.getId(), UserProfileProvider.class.getName()).findAny();
    }

    protected static void setDefaultConfiguration(KeycloakSession session) {
        setConfiguration(session, UPConfigUtils.readSystemDefaultConfig());
    }

    protected static void setConfiguration(KeycloakSession session, String config) {
        UserProfileProvider provider = getUserProfileProvider(session);
        try {
            UPConfig upConfig = config == null ? null : UPConfigUtils.parseConfig(config);
            provider.setConfiguration(upConfig);
        } catch (IOException ioe) {
            throw new RuntimeException("Error when parsing user-profile config: " + config, ioe);
        }
    }

    protected static UserProfileProvider getUserProfileProvider(KeycloakSession session) {
        return session.getProvider(UserProfileProvider.class);
    }

    /**
     * Generate big configuration to test slicing in the persistence/component config
     * @return a configuration that is expected to be split into 2 slices
     * @throws IOException
     */
    protected static String generateLargeProfileConfig() throws IOException {
        
        UPConfig config = new UPConfig();
        for (int i = 0; i < 80; i++) {
            UPAttribute attribute = new UPAttribute();
            attribute.setName(UserModel.USERNAME+i);
            Map<String, Object> validatorConfig = new HashMap<>();
            validatorConfig.put("min", 3);
            attribute.addValidation("length", validatorConfig);
            config.addOrReplaceAttribute(attribute);
        }
        String newConfig = JsonSerialization.writeValueAsString(config);
        return newConfig;
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
                if (OAuth2Constants.SCOPE.equals(name) && scopes != null && !scopes.isEmpty()) {
                    return String.join(" ", scopes);
                } else {
                    return null;
                }
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
    }
}
