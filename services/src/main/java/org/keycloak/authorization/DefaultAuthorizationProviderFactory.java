/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.authorization.policy.evaluation.DefaultPolicyEvaluator;
import org.keycloak.authorization.policy.evaluation.PolicyEvaluator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DefaultAuthorizationProviderFactory implements AuthorizationProviderFactory {

    private final PolicyEvaluator policyEvaluator = new DefaultPolicyEvaluator();
    private int inParametersLimitThreshold;

    @Override
    public AuthorizationProvider create(KeycloakSession session) {
        return create(session, session.getContext().getRealm());
    }

    @Override
    public void init(Config.Scope config) {
        this.inParametersLimitThreshold = config.getInt(
                JPA_IN_PARAMETERS_LIMIT_THRESHOLD.getName(),
                (Integer) JPA_IN_PARAMETERS_LIMIT_THRESHOLD.getDefaultValue());
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "authorization";
    }

    @Override
    public AuthorizationProvider create(KeycloakSession session, RealmModel realm) {
        return new AuthorizationProvider(session, realm, policyEvaluator);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C> C getConfig(ProviderConfigProperty property) {
        if (JPA_IN_PARAMETERS_LIMIT_THRESHOLD.getName().equals(property.getName())) {
            return (C) Integer.valueOf(inParametersLimitThreshold);
        }
        return null;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return List.of(JPA_IN_PARAMETERS_LIMIT_THRESHOLD);
    }
}
