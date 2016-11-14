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

import org.keycloak.authorization.permission.evaluator.Evaluators;
import org.keycloak.authorization.policy.evaluation.DefaultPolicyEvaluator;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * <p>The main contract here is the creation of {@link org.keycloak.authorization.permission.evaluator.PermissionEvaluator} instances.  Usually
 * an application has a single {@link AuthorizationProvider} instance and threads servicing client requests obtain {@link org.keycloak.authorization.core.permission.evaluator.PermissionEvaluator}
 * from the {@link #evaluators()} method.
 *
 * <p>The internal state of a {@link AuthorizationProvider} is immutable.  This internal state includes all of the metadata
 * used during the evaluation of policies.
 *
 * <p>Once created, {@link org.keycloak.authorization.permission.evaluator.PermissionEvaluator} instances can be obtained from the {@link #evaluators()} method:
 *
 * <pre>
 *     List<ResourcePermission> permissionsToEvaluate = getPermissions(); // the permissions to evaluate
 *     EvaluationContext evaluationContext = createEvaluationContext(); // the context with runtime environment information
 *     PermissionEvaluator evaluator = authorization.evaluators().from(permissionsToEvaluate, context);
 *
 *     evaluator.evaluate(new Decision() {
 *
 *         public void onDecision(Evaluation evaluation) {
 *              // do something on grant
 *         }
 *
 *     });
 * </pre>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class AuthorizationProvider implements Provider {

    private final DefaultPolicyEvaluator policyEvaluator;
    private final Executor scheduller;
    private final StoreFactory storeFactory;
    private final List<PolicyProviderFactory> policyProviderFactories;
    private final KeycloakSession keycloakSession;
    private final RealmModel realm;

    public AuthorizationProvider(KeycloakSession session, RealmModel realm, StoreFactory storeFactory, Executor scheduller) {
        this.keycloakSession = session;
        this.realm = realm;
        this.storeFactory = storeFactory;
        this.scheduller = scheduller;
        this.policyProviderFactories = configurePolicyProviderFactories(session);
        this.policyEvaluator = new DefaultPolicyEvaluator(this, this.policyProviderFactories);
    }

    public AuthorizationProvider(KeycloakSession session, RealmModel realm, StoreFactory storeFactory) {
        this(session, realm, storeFactory, Runnable::run);
    }

    /**
     * Returns a {@link Evaluators} instance from where {@link org.keycloak.authorization.policy.evaluation.PolicyEvaluator} instances
     * can be obtained.
     *
     * @return a {@link Evaluators} instance
     */
    public Evaluators evaluators() {
        return new Evaluators(this.policyProviderFactories, this.policyEvaluator, this.scheduller);
    }

    /**
     * Returns a {@link StoreFactory}.
     *
     * @return the {@link StoreFactory}
     */
    public StoreFactory getStoreFactory() {
        return this.storeFactory;
    }

    /**
     * Returns the registered {@link PolicyProviderFactory}.
     *
     * @return a {@link List} containing all registered {@link PolicyProviderFactory}
     */
    public List<PolicyProviderFactory> getProviderFactories() {
        return this.policyProviderFactories;
    }

    /**
     * Returns a {@link PolicyProviderFactory} given a <code>type</code>.
     *
     * @param type the type of the policy provider
     * @param <F> the expected type of the provider
     * @return a {@link PolicyProviderFactory} with the given <code>type</code>
     */
    public <F extends PolicyProviderFactory> F getProviderFactory(String type) {
        return (F) getProviderFactories().stream().filter(policyProviderFactory -> policyProviderFactory.getId().equals(type)).findFirst().orElse(null);
    }

    public KeycloakSession getKeycloakSession() {
        return this.keycloakSession;
    }

    public RealmModel getRealm() {
        return realm;
    }

    private List<PolicyProviderFactory> configurePolicyProviderFactories(KeycloakSession session) {
        List<ProviderFactory> providerFactories = session.getKeycloakSessionFactory().getProviderFactories(PolicyProvider.class);

        if (providerFactories.isEmpty()) {
            throw new RuntimeException("Could not find any policy provider.");
        }

        return providerFactories.stream().map(providerFactory -> (PolicyProviderFactory) providerFactory).collect(Collectors.toList());
    }

    @Override
    public void close() {

    }
}
