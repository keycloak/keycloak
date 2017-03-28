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

package org.keycloak.testsuite.client;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.keycloak.testsuite.client.resources.TestApplicationResource;
import org.keycloak.testsuite.client.resources.TestExampleCompanyResource;
import org.keycloak.testsuite.client.resources.TestingResource;
import org.keycloak.testsuite.runonserver.*;
import org.keycloak.util.JsonSerialization;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KeycloakTestingClient {

    private final ResteasyWebTarget target;
    private final ResteasyClient client;
    private static final boolean authServerSslRequired = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required"));

    KeycloakTestingClient(String serverUrl, ResteasyClient resteasyClient) {
        client = resteasyClient != null ? resteasyClient : newResteasyClientBuilder().connectionPoolSize(10).build();
        target = client.target(serverUrl);
    }

    private static ResteasyClientBuilder newResteasyClientBuilder() {
        if (authServerSslRequired) {
            // Disable PKIX path validation errors when running tests using SSL
            HostnameVerifier hostnameVerifier = (hostName, session) -> true;
            return new ResteasyClientBuilder().disableTrustManager().hostnameVerifier(hostnameVerifier);
        }
        return new ResteasyClientBuilder();
    }

    public static KeycloakTestingClient getInstance(String serverUrl) {
        return new KeycloakTestingClient(serverUrl, null);
    }

    public TestingResource testing() {
        return target.path("/realms/master").proxy(TestingResource.class);
    }

    public TestingResource testing(String realm) {
        return target.path("/realms/" + realm).proxy(TestingResource.class);
    }

    public TestApplicationResource testApp() { return target.proxy(TestApplicationResource.class); }

    public TestExampleCompanyResource testExampleCompany() { return target.proxy(TestExampleCompanyResource.class); }

    public Server server() {
        return new Server("master");
    }

    public Server server(String realm) {
        return new Server(realm);
    }

    public class Server {

        private String realm;

        public Server(String realm) {
            this.realm = realm;
        }

        public <T> T fetch(FetchOnServerWrapper<T> wrapper) throws RunOnServerException {
            return fetch(wrapper.getRunOnServer(), wrapper.getResultClass());
        }

        public <T> T fetch(FetchOnServer function, Class<T> clazz) throws RunOnServerException {
            try {
                String s = fetch(function);
                return JsonSerialization.readValue(s, clazz);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public String fetch(FetchOnServer function) throws RunOnServerException {
            String encoded = SerializationUtil.encode(function);

            String result = testing(realm != null ? realm : "master").runOnServer(encoded);
            if (result != null && !result.isEmpty() && !result.trim().startsWith("{")) {
                Throwable t = SerializationUtil.decodeException(result);
                if (t instanceof AssertionError) {
                    throw (AssertionError) t;
                } else {
                    throw new RunOnServerException(t);
                }
            } else {
                return result;
            }
        }

        public void run(RunOnServer function) throws RunOnServerException {
            String encoded = SerializationUtil.encode(function);

            String result = testing(realm != null ? realm : "master").runOnServer(encoded);
            if (result != null && !result.isEmpty() && !result.trim().startsWith("{")) {
                Throwable t = SerializationUtil.decodeException(result);
                if (t instanceof AssertionError) {
                    throw (AssertionError) t;
                } else {
                    throw new RunOnServerException(t);
                }
            }
        }

    }

    public void close() {
        client.close();
    }
}
