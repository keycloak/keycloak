/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

public class ModelToRepresentationTest {

    @Test
    public void exportPolicyWithMissingProviderFactoryDoesNotThrowAndPreservesConfig() {
        Map<String, String> storedConfig = Map.of("code", "$evaluation.grant();");
        Policy policy = stubPolicy("js", "Default Policy", storedConfig);
        AuthorizationProvider authorization = stubAuthorizationWithFactory(null);

        PolicyRepresentation rep = ModelToRepresentation.toRepresentation(
                policy, authorization, false, true, false);

        // export completed without throwing
        assertThat(rep, is(notNullValue()));
        // stored config preserved for round-trip
        assertThat(rep.getConfig(), is(equalTo(storedConfig)));
    }

    @Test
    public void exportPolicyWithProviderFactoryDelegatesToOnExport() {
        AtomicReference<Policy> onExportPolicy = new AtomicReference<>();
        AtomicReference<PolicyRepresentation> onExportRepresentation = new AtomicReference<>();

        Policy policy = stubPolicy("role", "My Role Policy", Map.of());
        PolicyProviderFactory factory = (PolicyProviderFactory) Proxy.newProxyInstance(
                PolicyProviderFactory.class.getClassLoader(),
                new Class<?>[] { PolicyProviderFactory.class },
                (proxy, method, args) -> {
                    if ("onExport".equals(method.getName())) {
                        onExportPolicy.set((Policy) args[0]);
                        onExportRepresentation.set((PolicyRepresentation) args[1]);
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        AuthorizationProvider authorization = stubAuthorizationWithFactory(factory);

        PolicyRepresentation rep = ModelToRepresentation.toRepresentation(
                policy, authorization, false, true, false);

        // export completed without throwing
        assertThat(rep, is(notNullValue()));
        assertThat(onExportPolicy.get(), is(sameInstance(policy)));
        // onExport receives the live representation to mutate
        assertThat(onExportRepresentation.get(), is(sameInstance(rep)));
    }

    private static Policy stubPolicy(String type, String name, Map<String, String> config) {
        return (Policy) Proxy.newProxyInstance(
                Policy.class.getClassLoader(),
                new Class<?>[] { Policy.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getType" -> type;
                    case "getName" -> name;
                    case "getConfig" -> config;
                    case "getId", "getDescription", "getDecisionStrategy", "getLogic" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static AuthorizationProvider stubAuthorizationWithFactory(PolicyProviderFactory factory) {
        InvocationHandler factoryHandler = (proxy, method, args) -> {
            if ("getProviderFactory".equals(method.getName())
                    && args != null && args.length >= 1
                    && PolicyProvider.class.equals(args[0])) {
                return factory;
            }
            throw new UnsupportedOperationException(method.getName());
        };
        KeycloakSessionFactory sessionFactory = (KeycloakSessionFactory) Proxy.newProxyInstance(
                KeycloakSessionFactory.class.getClassLoader(),
                new Class<?>[] { KeycloakSessionFactory.class },
                factoryHandler);

        KeycloakSession session = (KeycloakSession) Proxy.newProxyInstance(
                KeycloakSession.class.getClassLoader(),
                new Class<?>[] { KeycloakSession.class },
                (proxy, method, args) -> {
                    if ("getKeycloakSessionFactory".equals(method.getName())) {
                        return sessionFactory;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        // realm and policyEvaluator are not exercised by the export branch under test
        return new AuthorizationProvider(session, null, null);
    }
}
