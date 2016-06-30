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
package org.keycloak.testsuite.authorization;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import static org.bouncycastle.asn1.x500.style.RFC4519Style.l;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class TestPolicyProviderFactory implements PolicyProviderFactory {

    @Override
    public String getName() {
        return "Test";
    }

    @Override
    public String getGroup() {
        return "Test Suite";
    }

    @Override
    public PolicyProvider create(Policy policy, AuthorizationProvider authorization) {
        return new TestPolicyProvider(policy, authorization);
    }

    @Override
    public PolicyProviderAdminService getAdminResource(ResourceServer resourceServer) {
        return null;
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return null;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "test";
    }

    private class TestPolicyProvider implements PolicyProvider {

        private final Policy policy;
        private final AuthorizationProvider authorization;

        public TestPolicyProvider(Policy policy, AuthorizationProvider authorization) {
            this.policy = policy;
            this.authorization = authorization;
        }

        @Override
        public void evaluate(Evaluation evaluation) {

        }

        @Override
        public void close() {

        }
    }
}
