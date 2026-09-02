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

package org.keycloak.client.registration;

import java.io.IOException;
import java.io.InputStream;

import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegistration {

    public static final ObjectMapper outputMapper = new ObjectMapper();
    static {
        outputMapper.addMixIn(ClientRepresentation.class, ClientRepresentationMixIn.class);
        outputMapper.addMixIn(OIDCClientRepresentation.class, OIDCClientRepresentationMixIn.class);
        outputMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private final String JSON = "application/json";
    private final String XML = "application/xml";

    private final String DEFAULT = "default";
    private final String INSTALLATION = "install";
    private final String OIDC = "openid-connect";
    private final String SAML = "saml2-entity-descriptor";

    private HttpUtil httpUtil;

    public static ClientRegistrationBuilder create() {
        return new ClientRegistrationBuilder();
    }

    ClientRegistration(HttpUtil httpUtil) {
        this.httpUtil = httpUtil;
    }

    public void close() throws ClientRegistrationException {
        if (httpUtil != null) {
            httpUtil.close();
        }
        httpUtil = null;
    }

    public ClientRegistration auth(Auth auth) {
        httpUtil.setAuth(auth);
        return this;
    }

    public ClientRepresentation create(ClientRepresentation client) throws ClientRegistrationException {
        String content = serialize(client);
        InputStream resultStream = httpUtil.doPost(content, JSON, UTF_8, JSON, DEFAULT);
        return deserialize(resultStream, ClientRepresentation.class);
    }

    public ClientRepresentation get(String clientId) throws ClientRegistrationException {
        InputStream resultStream = httpUtil.doGet(JSON, DEFAULT, clientId);
        return resultStream != null ? deserialize(resultStream, ClientRepresentation.class) : null;
    }

    public AdapterConfig getAdapterConfig(String clientId) throws ClientRegistrationException {
        InputStream resultStream = httpUtil.doGet(JSON, INSTALLATION, clientId);
        return resultStream != null ? deserialize(resultStream, AdapterConfig.class) : null;
    }

    public ClientRepresentation update(ClientRepresentation client) throws ClientRegistrationException {
        String content = serialize(client);
        InputStream resultStream = httpUtil.doPut(content, JSON, UTF_8, JSON, DEFAULT, client.getClientId());
        return resultStream != null ? deserialize(resultStream, ClientRepresentation.class) : null;
    }

    public void delete(ClientRepresentation client) throws ClientRegistrationException {
        delete(client.getClientId());
    }

    public void delete(String clientId) throws ClientRegistrationException {
        httpUtil.doDelete(DEFAULT, clientId);
    }

    public OIDCClientRegistration oidc() {
        return new OIDCClientRegistration();
    }

    public SAMLClientRegistration saml() {
        return new SAMLClientRegistration();
    }

    public static String serialize(Object obj) throws ClientRegistrationException {
        try {
            return outputMapper.writeValueAsString(obj);
        } catch (IOException e) {
            throw new ClientRegistrationException("Failed to write json object", e);
        }
    }

    private static <T> T deserialize(InputStream inputStream, Class<T> clazz) throws ClientRegistrationException {
        try {
            return JsonSerialization.readValue(inputStream, clazz);
        } catch (IOException e) {
            throw new ClientRegistrationException("Failed to read json object", e);
        }
    }

    public class OIDCClientRegistration {

        public OIDCClientRepresentation create(OIDCClientRepresentation client) throws ClientRegistrationException {
            String content = serialize(client);
            InputStream resultStream = httpUtil.doPost(content, JSON, UTF_8, JSON, OIDC);
            return deserialize(resultStream, OIDCClientRepresentation.class);
        }

        public OIDCClientRepresentation get(String clientId) throws ClientRegistrationException {
            InputStream resultStream = httpUtil.doGet(JSON, OIDC, clientId);
            return resultStream != null ? deserialize(resultStream, OIDCClientRepresentation.class) : null;
        }

        public OIDCClientRepresentation update(OIDCClientRepresentation client) throws ClientRegistrationException {
            String content = serialize(client);
            InputStream resultStream = httpUtil.doPut(content, JSON, UTF_8, JSON, OIDC, client.getClientId());
            return resultStream != null ? deserialize(resultStream, OIDCClientRepresentation.class) : null;
        }

        public void delete(OIDCClientRepresentation client) throws ClientRegistrationException {
            delete(client.getClientId());
        }

        public void delete(String clientId) throws ClientRegistrationException {
            httpUtil.doDelete(OIDC, clientId);
        }

    }

    public class SAMLClientRegistration {

        public ClientRepresentation create(String entityDescriptor) throws ClientRegistrationException {
            InputStream resultStream = httpUtil.doPost(entityDescriptor, XML, UTF_8, JSON, SAML);
            return deserialize(resultStream, ClientRepresentation.class);
        }

    }

    public static class ClientRegistrationBuilder {

        private String url;
        private HttpClient httpClient;

        ClientRegistrationBuilder() {
        }

        public ClientRegistrationBuilder url(String realmUrl) {
            url = realmUrl;
            return this;
        }

        public ClientRegistrationBuilder url(String authUrl, String realm) {
            url = HttpUtil.getUrl(authUrl, "realms", realm, "clients-registrations");
            return this;
        }

        public ClientRegistrationBuilder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public ClientRegistration build() {
            if (url == null) {
                throw new IllegalStateException("url not configured");
            }

            if (httpClient == null) {
                httpClient = HttpClients.createDefault();
            }

            return new ClientRegistration(new HttpUtil(httpClient, url));
        }

    }

}
