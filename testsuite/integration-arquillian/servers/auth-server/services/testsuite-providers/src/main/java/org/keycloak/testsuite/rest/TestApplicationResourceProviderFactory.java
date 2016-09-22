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

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.adapters.action.TestAvailabilityAction;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TestApplicationResourceProviderFactory implements RealmResourceProviderFactory {

    private BlockingQueue<LogoutAction> adminLogoutActions = new LinkedBlockingDeque<>();
    private BlockingQueue<PushNotBeforeAction> pushNotBeforeActions = new LinkedBlockingDeque<>();
    private BlockingQueue<TestAvailabilityAction> testAvailabilityActions = new LinkedBlockingDeque<>();

    private final OIDCClientData oidcClientData = new OIDCClientData();

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new TestApplicationResourceProvider(session, adminLogoutActions, pushNotBeforeActions, testAvailabilityActions, oidcClientData);
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

        private KeyPair signingKeyPair;
        private String oidcRequest;
        private List<String> sectorIdentifierRedirectUris;

        public KeyPair getSigningKeyPair() {
            return signingKeyPair;
        }

        public void setSigningKeyPair(KeyPair signingKeyPair) {
            this.signingKeyPair = signingKeyPair;
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
}
