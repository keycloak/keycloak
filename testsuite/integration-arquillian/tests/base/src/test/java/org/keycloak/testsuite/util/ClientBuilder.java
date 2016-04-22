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

import org.codehaus.plexus.util.xml.pull.XmlSerializer;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.client.Client;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientBuilder {

    private ClientRepresentation rep = new ClientRepresentation();

    public static ClientBuilder create() {
        return new ClientBuilder();
    }

    private ClientBuilder() {
        rep.setEnabled(true);
    }

    public ClientBuilder id(String id) {
        rep.setId(id);
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

    public ClientBuilder directAccessGrants() {
        rep.setDirectAccessGrantsEnabled(true);
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

    public ClientBuilder redirectUris(String... redirectUris) {
        rep.setRedirectUris(Arrays.asList(redirectUris));
        return this;
    }
}
