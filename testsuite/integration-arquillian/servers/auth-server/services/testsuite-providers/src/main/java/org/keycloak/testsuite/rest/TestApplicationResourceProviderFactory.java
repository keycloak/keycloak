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

package org.keycloak.testsuite.rest;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;

import org.keycloak.Config.Scope;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oidc.grants.ciba.endpoints.ClientNotificationEndpointRequest;
import org.keycloak.representations.LogoutToken;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.adapters.action.TestAvailabilityAction;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;
import org.keycloak.testsuite.rest.representation.TestAuthenticationChannelRequest;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TestApplicationResourceProviderFactory implements RealmResourceProviderFactory {

    private BlockingQueue<LogoutAction> adminLogoutActions = new LinkedBlockingDeque<>();
    private BlockingQueue<String> backChannelLogoutTokens = new LinkedBlockingDeque<>();
    private BlockingQueue<LogoutToken> frontChannelLogoutTokens = new LinkedBlockingDeque<>();
    private BlockingQueue<PushNotBeforeAction> pushNotBeforeActions = new LinkedBlockingDeque<>();
    private BlockingQueue<TestAvailabilityAction> testAvailabilityActions = new LinkedBlockingDeque<>();

    private final OIDCClientData oidcClientData = new OIDCClientData();
    private ConcurrentMap<String, TestAuthenticationChannelRequest> authenticationChannelRequests = new ConcurrentHashMap<>();
    private ConcurrentMap<String, ClientNotificationEndpointRequest> cibaClientNotifications = new ConcurrentHashMap<>();
    private ConcurrentMap<String, String> intentClientBindings = new ConcurrentHashMap<>();

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new TestApplicationResourceProvider(session, adminLogoutActions,
                backChannelLogoutTokens, frontChannelLogoutTokens, pushNotBeforeActions, testAvailabilityActions, oidcClientData, authenticationChannelRequests, cibaClientNotifications, intentClientBindings);
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "app";
    }


    public static class OIDCClientData {

        private List<OIDCKeyData> keys = new ArrayList<>();

        private String oidcRequest;
        private List<String> sectorIdentifierRedirectUris;

        public List<OIDCKeyData> getKeys() {
            return keys;
        }

        public OIDCKeyData getFirstKey() {
            return keys.isEmpty() ? null : keys.get(0);
        }

        public void addKey(OIDCKeyData key, boolean keepExistingKeys) {
            if (!keepExistingKeys) {
                this.keys = new ArrayList<>();
            }
            this.keys.add(0, key);
        }

        public String getOidcRequest() {
            return oidcRequest;
        }

        public void setOidcRequest(String oidcRequest) {
            this.oidcRequest = oidcRequest;
        }

        public List<String> getSectorIdentifierRedirectUris() {
            return sectorIdentifierRedirectUris;
        }

        public void setSectorIdentifierRedirectUris(List<String> sectorIdentifierRedirectUris) {
            this.sectorIdentifierRedirectUris = sectorIdentifierRedirectUris;
        }

    }

    public static class OIDCKeyData {

        private KeyPair keyPair;

        private String keyType = KeyType.RSA;
        private String keyAlgorithm;
        private KeyUse keyUse = KeyUse.SIG;
        private String curve;

        // Kid will be randomly generated (based on the key hash) if not provided here
        private String kid;

        public KeyPair getSigningKeyPair() {
            return keyPair;
        }

        public void setSigningKeyPair(KeyPair signingKeyPair) {
            this.keyPair = signingKeyPair;
        }

        public String getSigningKeyType() {
            return keyType;
        }

        public void setSigningKeyType(String signingKeyType) {
            this.keyType = signingKeyType;
        }

        public String getSigningKeyAlgorithm() {
            return keyAlgorithm;
        }

        public void setSigningKeyAlgorithm(String signingKeyAlgorithm) {
            this.keyAlgorithm = signingKeyAlgorithm;
        }

        public KeyPair getKeyPair() {
            return keyPair;
        }

        public void setKeyPair(KeyPair keyPair) {
            this.keyPair = keyPair;
        }

        public String getKeyType() {
            return keyType;
        }

        public void setKeyType(String keyType) {
            this.keyType = keyType;
        }

        public String getKeyAlgorithm() {
            return keyAlgorithm;
        }

        public void setKeyAlgorithm(String keyAlgorithm) {
            this.keyAlgorithm = keyAlgorithm;
        }

        public KeyUse getKeyUse() {
            return keyUse;
        }

        public void setKeyUse(KeyUse keyUse) {
            this.keyUse = keyUse;
        }

        public String getKid() {
            return kid;
        }

        public void setKid(String kid) {
            this.kid = kid;
        }

        public String getCurve() {
            return curve;
        }

        public void setCurve(String curve) {
            this.curve = curve;
        }
    }
}
