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

package org.keycloak.testsuite.util;

import java.util.ArrayList;
import org.keycloak.dom.saml.v2.ac.BooleanType;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientBuilder {

    private ClientRepresentation rep;

    public static ClientBuilder create() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setEnabled(Boolean.TRUE);
        return new ClientBuilder(rep);
    }

    public static ClientBuilder edit(ClientRepresentation rep) {
        return new ClientBuilder(rep);
    }

    private ClientBuilder(ClientRepresentation rep) {
        this.rep = rep;
    }

    public ClientBuilder id(String id) {
        rep.setId(id);
        return this;
    }

    public ClientBuilder name(String name) {
        rep.setName(name);
        return this;
    }

    public ClientBuilder clientId(String clientId) {
        rep.setClientId(clientId);
        return this;
    }

    public ClientBuilder publicClient() {
        rep.setPublicClient(true);
        return this;
    }

    public ClientBuilder defaultRoles(String... roles) {
        rep.setDefaultRoles(roles);
        return this;
    }

    public ClientBuilder serviceAccount() {
        rep.setServiceAccountsEnabled(true);
        return this;
    }

    public ClientBuilder directAccessGrants() {
        rep.setDirectAccessGrantsEnabled(true);
        return this;
    }

    public ClientBuilder fullScopeEnabled(Boolean fullScopeEnabled) {
        rep.setFullScopeAllowed(fullScopeEnabled);
        return this;
    }

    public ClientBuilder secret(String secret) {
        rep.setSecret(secret);
        return this;
    }

    public ClientBuilder serviceAccountsEnabled(Boolean serviceAccountsEnabled) {
        rep.setServiceAccountsEnabled(serviceAccountsEnabled);
        return this;
    }

    public ClientRepresentation build() {
        return rep;
    }

    public ClientBuilder attribute(String name, String value) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(name, value);
        rep.setAttributes(attributes);
        return this;
    }

    public ClientBuilder authenticatorType(String providerId) {
        rep.setClientAuthenticatorType(providerId);
        return this;
    }

    public ClientBuilder addWebOrigin(String webOrigin) {
        List<String> uris = rep.getWebOrigins();
        if (uris == null) {
            uris = new LinkedList<>();
            rep.setWebOrigins(uris);
        }
        uris.add(webOrigin);
        return this;
    }

    public ClientBuilder addRedirectUri(String redirectUri) {
        List<String> uris = rep.getRedirectUris();
        if (uris == null) {
            uris = new LinkedList<>();
            rep.setRedirectUris(uris);
        }
        uris.add(redirectUri);
        return this;
    }

    public ClientBuilder redirectUris(String... redirectUris) {
        rep.setRedirectUris(Arrays.asList(redirectUris));
        return this;
    }

    public ClientBuilder baseUrl(String baseUrl) {
        rep.setBaseUrl(baseUrl);
        return this;
    }

    public ClientBuilder adminUrl(String adminUrl) {
        rep.setAdminUrl(adminUrl);
        return this;
    }

    public ClientBuilder rootUrl(String rootUrl) {
        rep.setRootUrl(rootUrl);
        return this;
    }
}
