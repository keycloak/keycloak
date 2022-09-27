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

import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.keycloak.common.Profile;
import org.keycloak.testsuite.client.resources.TestApplicationResource;
import org.keycloak.testsuite.client.resources.TestExampleCompanyResource;
import org.keycloak.testsuite.client.resources.TestSamlApplicationResource;
import org.keycloak.testsuite.client.resources.TestingResource;
import org.keycloak.testsuite.runonserver.*;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.util.JsonSerialization;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KeycloakTestingClient implements AutoCloseable {

    private final ResteasyWebTarget target;
    private final ResteasyClient client;

    KeycloakTestingClient(String serverUrl, ResteasyClient resteasyClient) {
        if (resteasyClient != null) {
            client = resteasyClient;
        } else {
            ResteasyClientBuilder resteasyClientBuilder = (ResteasyClientBuilder) ResteasyClientBuilder.newBuilder();
            resteasyClientBuilder.connectionPoolSize(10);
            if (serverUrl.startsWith("https")) {
                // Disable PKIX path validation errors when running tests using SSL
                resteasyClientBuilder.disableTrustManager().hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY);
            }
            resteasyClientBuilder.httpEngine(AdminClientUtil.getCustomClientHttpEngine(resteasyClientBuilder, 10, null));
            client = resteasyClientBuilder.build();
        }
        target = client.target(serverUrl);
    }

    public static KeycloakTestingClient getInstance(String serverUrl) {
        return new KeycloakTestingClient(serverUrl, null);
    }

    public static KeycloakTestingClient getInstance(String serverUrl, ResteasyClient resteasyClient) {
        return new KeycloakTestingClient(serverUrl, resteasyClient);
    }

    public TestingResource testing() {
        return target.path("/realms/master").proxy(TestingResource.class);
    }

    public TestingResource testing(String realm) {
        return target.path("/realms/" + realm).proxy(TestingResource.class);
    }

    public void enableFeature(Profile.Feature feature) {
        try (Response response = testing().enableFeature(feature.toString())) {
            assertEquals(204, response.getStatus());
        }
    }

    public void disableFeature(Profile.Feature feature) {
        try (Response response = testing().disableFeature(feature.toString())) {
            assertEquals(204, response.getStatus());
        }
    }

    public TestApplicationResource testApp() { return target.proxy(TestApplicationResource.class); }

    public TestSamlApplicationResource testSamlApp() { return target.proxy(TestSamlApplicationResource.class); }

    public TestExampleCompanyResource testExampleCompany() { return target.proxy(TestExampleCompanyResource.class); }

    /**
     * Allows running code on the server-side for white-box testing. When using be careful what imports your test class
     * has and also what classes are used within the function sent to the server. Classes have to be either available
     * server-side or defined in @{@link org.keycloak.testsuite.arquillian.TestClassProvider#PERMITTED_PACKAGES}
     *
     * @return
     */
    public Server server() {
        return new Server("master");
    }

    public Server server(String realm) {
        return new Server(realm);
    }

    public class Server {

        private final String realm;

        public Server(String realm) {
            this.realm = realm;
        }

        public <T> T fetch(FetchOnServerWrapper<T> wrapper) throws RunOnServerException {
            return fetch(wrapper.getRunOnServer(), wrapper.getResultClass());
        }

        public <T> T fetch(FetchOnServer function, Class<T> clazz) throws RunOnServerException {
            try {
                String s = fetchString(function);
                return s==null ? null : JsonSerialization.readValue(s, clazz);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public String fetchString(FetchOnServer function) throws RunOnServerException {
            String encoded = SerializationUtil.encode(function);

            String result = testing(realm != null ? realm : "master").runOnServer(encoded);
            if (result != null && !result.isEmpty() && result.trim().startsWith("EXCEPTION:")) {
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
            if (result != null && !result.isEmpty() && result.trim().startsWith("EXCEPTION:")) {
                Throwable t = SerializationUtil.decodeException(result);
                if (t instanceof AssertionError) {
                    throw (AssertionError) t;
                } else {
                    throw new RunOnServerException(t);
                }
            }
        }

        public void runModelTest(String testClassName, String testMethodName) throws RunOnServerException {
            String result = testing(realm != null ? realm : "master").runModelTestOnServer(testClassName, testMethodName);

            if (result != null && !result.isEmpty() && result.trim().startsWith("EXCEPTION:")) {
                Throwable t = SerializationUtil.decodeException(result);

                if (t instanceof AssertionError) {
                    throw (AssertionError) t;
                } else {
                    throw new RunOnServerException(t);
                }
            }
        }

    }

    @Override
    public void close() {
        client.close();
    }
}
