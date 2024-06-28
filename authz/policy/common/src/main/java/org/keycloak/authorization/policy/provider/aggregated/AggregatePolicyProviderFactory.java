/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.authorization.policy.provider.aggregated;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AggregatePolicyProviderFactory implements PolicyProviderFactory<AggregatePolicyRepresentation> {

    private AggregatePolicyProvider provider = new AggregatePolicyProvider();

    @Override
    public String getName() {
        return "Aggregated";
    }

    @Override
    public String getGroup() {
        return "Others";
    }

    @Override
    public PolicyProvider create(AuthorizationProvider authorization) {
        return provider;
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return null;
    }

    @Override
    public void onCreate(Policy policy, AggregatePolicyRepresentation representation, AuthorizationProvider authorization) {
        verifyCircularReference(policy, new ArrayList<>());
    }

    @Override
    public void onUpdate(Policy policy, AggregatePolicyRepresentation representation, AuthorizationProvider authorization) {
        verifyCircularReference(policy, new ArrayList<>());
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        verifyCircularReference(policy, new ArrayList<>());
    }

    @Override
    public AggregatePolicyRepresentation toRepresentation(Policy policy, AuthorizationProvider authorization) {
        return new AggregatePolicyRepresentation();
    }

    @Override
    public Class<AggregatePolicyRepresentation> getRepresentationType() {
        return AggregatePolicyRepresentation.class;
    }

    private void verifyCircularReference(Policy policy, List<String> ids) {
        if (!policy.getType().equals("aggregate")) {
            return;
        }

        if (ids.contains(policy.getId())) {
            throw new RuntimeException("Circular reference found [" + policy.getName() + "].");
        }

        ids.add(policy.getId());

        for (Policy associated : policy.getAssociatedPolicies()) {
            verifyCircularReference(associated, ids);
        }
    }

    @Override
    public void onRemove(Policy policy, AuthorizationProvider authorization) {

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
        return "aggregate";
    }
}
