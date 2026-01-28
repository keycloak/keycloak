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

import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.Profile;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.client.resources.TestApplicationResource;
import org.keycloak.testsuite.client.resources.TestExampleCompanyResource;
import org.keycloak.testsuite.client.resources.TestSamlApplicationResource;
import org.keycloak.testsuite.client.resources.TestingResource;
import org.keycloak.testsuite.runonserver.FetchOnServer;
import org.keycloak.testsuite.runonserver.FetchOnServerWrapper;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.testsuite.runonserver.RunOnServerException;
import org.keycloak.testsuite.runonserver.SerializationUtil;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.util.JsonSerialization;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.Assert;
import org.junit.AssumptionViolatedException;

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
            ResteasyClientBuilder resteasyClientBuilder = getRestEasyClientBuilder(serverUrl);
            client = resteasyClientBuilder.build();
        }
        target = client.target(serverUrl);
    }

    public static ResteasyClientBuilder getRestEasyClientBuilder(String serverUrl) {
        ResteasyClientBuilder resteasyClientBuilder = (ResteasyClientBuilder) ResteasyClientBuilder.newBuilder();
        resteasyClientBuilder.connectionPoolSize(10);
        if ((serverUrl != null && serverUrl.startsWith("https")) || "true".equals(System.getProperty("auth.server.ssl.required"))) {
            // Disable PKIX path validation errors when running tests using SSL
            resteasyClientBuilder.disableTrustManager().hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY);
        }
        resteasyClientBuilder.httpEngine(AdminClientUtil.getCustomClientHttpEngine(resteasyClientBuilder, 10, null));
        return resteasyClientBuilder;
    }

    public static ResteasyClientBuilder getRestEasyClientBuilder() {
        return getRestEasyClientBuilder(null);
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
        String featureString;
        if (shouldUseVersionedKey(feature)) {
            featureString = feature.getVersionedKey();
        } else {
            featureString = feature.getKey();
        }
        Set<Profile.Feature> disabledFeatures = testing().enableFeature(featureString);
        Assert.assertFalse(disabledFeatures.contains(feature));
        ProfileAssume.updateDisabledFeatures(disabledFeatures);
    }

    private boolean shouldUseVersionedKey(Profile.Feature feature) {
        return ((Profile.getFeatureVersions(feature.getUnversionedKey()).size() > 1) || (feature.getVersion() != 1));
    }

    public void disableFeature(Profile.Feature feature) {
        String featureString;
        if (shouldUseVersionedKey(feature)) {
            featureString = feature.getVersionedKey();
        } else {
            featureString = feature.getKey();
        }
        Set<Profile.Feature> disabledFeatures = testing().disableFeature(featureString);
        Assert.assertTrue(disabledFeatures.contains(feature));
        ProfileAssume.updateDisabledFeatures(disabledFeatures);
    }

    /**
     * Resets the feature to it's default setting.
     *
     * @param feature
     */
    public void resetFeature(Profile.Feature feature) {
        String featureString;
        if (shouldUseVersionedKey(feature)) {
            featureString = feature.getVersionedKey();
            Profile.Feature featureVersionHighestPriority = Profile.getFeatureVersions(feature.getUnversionedKey()).iterator().next();
            if (featureVersionHighestPriority.getType().equals(Profile.Feature.Type.DEFAULT)) {
                enableFeature(featureVersionHighestPriority);
            }
        } else {
            featureString = feature.getKey();
        }
        testing().resetFeature(featureString);
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

        public Response runWithResponse(RunOnServer function) throws RunOnServerException {
            String encoded = SerializationUtil.encode(function);
            return testing(realm != null ? realm : "master").runOnServerWithResponse(encoded);
        }

        public void runModelTest(String testClassName, String testMethodName) throws RunOnServerException {
            String result = testing(realm != null ? realm : "master").runModelTestOnServer(testClassName, testMethodName);

            if (result != null && !result.isEmpty() && result.trim().startsWith("EXCEPTION:")) {
                Throwable t = SerializationUtil.decodeException(result);

                if (t instanceof AssertionError) {
                    throw (AssertionError) t;
                } else if (t instanceof AssumptionViolatedException) {
                    throw (AssumptionViolatedException) t;
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
