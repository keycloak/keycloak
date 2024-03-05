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
package org.keycloak.adapters;

import org.junit.Test;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.adapters.config.AdapterConfig;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:brad.culley@spartasystems.com">Brad Culley</a>
 * @author <a href="mailto:john.ament@spartasystems.com">John D. Ament</a>
 */
public class KeycloakDeploymentTest {
    @Test
    public void shouldNotEnableOAuthQueryParamWhenIgnoreIsTrue() {
        KeycloakDeployment keycloakDeployment = new KeycloakDeploymentMock();
        keycloakDeployment.setIgnoreOAuthQueryParameter(true);
        assertFalse(keycloakDeployment.isOAuthQueryParameterEnabled());
    }

    @Test
    public void shouldEnableOAuthQueryParamWhenIgnoreIsFalse() {
        KeycloakDeployment keycloakDeployment = new KeycloakDeploymentMock();
        keycloakDeployment.setIgnoreOAuthQueryParameter(false);
        assertTrue(keycloakDeployment.isOAuthQueryParameterEnabled());
    }

    @Test
    public void shouldEnableOAuthQueryParamWhenIgnoreNotSet() {
        KeycloakDeployment keycloakDeployment = new KeycloakDeploymentMock();

        assertTrue(keycloakDeployment.isOAuthQueryParameterEnabled());
    }

    @Test
    public void stripDefaultPorts() {
        KeycloakDeployment keycloakDeployment = new KeycloakDeploymentMock();
        keycloakDeployment.setRealm("test");
        AdapterConfig config = new AdapterConfig();
        config.setAuthServerUrl("http://localhost:80/auth");

        keycloakDeployment.setAuthServerBaseUrl(config);

        assertEquals("http://localhost/auth", keycloakDeployment.getAuthServerBaseUrl());

        config.setAuthServerUrl("https://localhost:443/auth");
        keycloakDeployment.setAuthServerBaseUrl(config);

        assertEquals("https://localhost/auth", keycloakDeployment.getAuthServerBaseUrl());
    }

    @Test
    public void logoutUrlNotNullInMultiThreading() {
        final int numberOfAttempts = 15;
        IntStream.rangeClosed(1, numberOfAttempts).forEach(attemptIndex -> {
            KeycloakDeployment keycloakDeployment = createTestDeployment();

            // keep it pretty low to avoid unneeded pressure on CI yet be able to reproduce the KEYCLOAK-27489 issue
            final int numberOfThreads = 20;
            AtomicInteger threadsReceivedNull = new AtomicInteger(0);

            new MultipleThreads(numberOfThreads, () -> {
                if (keycloakDeployment.getLogoutUrl() == null) {
                    threadsReceivedNull.incrementAndGet();
                }
            }).run();

			assertEquals(
                String.format("No thread must receive null logout URL, but it was received during the attempt#%d/%d", attemptIndex, numberOfAttempts), 
                    0,
                    threadsReceivedNull.get()
            );
        });
    }

    private KeycloakDeployment createTestDeployment() {
        KeycloakDeployment keycloakDeployment = new KeycloakDeploymentMock();
        keycloakDeployment.setRealm("test");
        AdapterConfig config = new AdapterConfig();
        config.setAuthServerUrl("http://localhost:80/auth");
        keycloakDeployment.setAuthServerBaseUrl(config);
        return keycloakDeployment;
    }

    private static class MultipleThreads {
        private final int numberOfThreads;
        private final Runnable runnable;

        private MultipleThreads(int numberOfThreads, Runnable runnable) {
            this.numberOfThreads = numberOfThreads;
            this.runnable = runnable;
        }

        private void run() {
            ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
            // startLatch increases the concurrency
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

            IntStream.range(0, numberOfThreads).forEach(threadIndex ->
                service.submit(() -> {
                    try {
                        startLatch.await();
                        runnable.run();
                    }
                    catch (InterruptedException e) {
                        // point to improve: the exceptions can be collected to a concurrent list and fail the test
                        // not affecting the current test though
                        throw new RuntimeException("Failed to wait in the start latch", e);
                    }
                    finally {
                        finishLatch.countDown();
                    }
                })
            );
            // start actions in all threads at once
            startLatch.countDown();
            // wait until all threads complete
			try {
				finishLatch.await();
			} catch (InterruptedException e) {
                throw new RuntimeException("Failed to wait in the finish latch", e);
			}
		}
    }

    private static class KeycloakDeploymentMock extends KeycloakDeployment {

        @Override
        protected OIDCConfigurationRepresentation getOidcConfiguration(String discoveryUrl) {
            String base = KeycloakUriBuilder.fromUri(discoveryUrl).replacePath("/auth").build().toString();

            OIDCConfigurationRepresentation rep = new OIDCConfigurationRepresentation();
            rep.setAuthorizationEndpoint(base + "/realms/test/authz");
            rep.setTokenEndpoint(base + "/realms/test/tokens");
            rep.setIssuer(base + "/realms/test");
            rep.setJwksUri(base + "/realms/test/jwks");
            rep.setLogoutEndpoint(base + "/realms/test/logout");
            return rep;
        }
    }
}
