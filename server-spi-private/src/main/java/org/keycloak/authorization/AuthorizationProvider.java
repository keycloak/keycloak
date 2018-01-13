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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.evaluator.Evaluators;
import org.keycloak.authorization.policy.evaluation.DefaultPolicyEvaluator;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.authorization.CachedStoreFactoryProvider;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.provider.Provider;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;

/**
 * <p>The main contract here is the creation of {@link org.keycloak.authorization.permission.evaluator.PermissionEvaluator} instances.  Usually
 * an application has a single {@link AuthorizationProvider} instance and threads servicing client requests obtain {@link org.keycloak.authorization.permission.evaluator.PermissionEvaluator}
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
    private StoreFactory storeFactory;
    private StoreFactory storeFactoryDelegate;
    private final Map<String, PolicyProviderFactory> policyProviderFactories;
    private final KeycloakSession keycloakSession;
    private final RealmModel realm;

    public AuthorizationProvider(KeycloakSession session, RealmModel realm, Map<String, PolicyProviderFactory> policyProviderFactories) {
        this.keycloakSession = session;
        this.realm = realm;
        this.policyProviderFactories = policyProviderFactories;
        this.policyEvaluator = new DefaultPolicyEvaluator(this);
    }

    /**
     * Returns a {@link Evaluators} instance from where {@link org.keycloak.authorization.policy.evaluation.PolicyEvaluator} instances
     * can be obtained.
     *
     * @return a {@link Evaluators} instance
     */
    public Evaluators evaluators() {
        return new Evaluators(policyEvaluator);
    }

    /**
     * Cache sits in front of this
     *
     * Returns a {@link StoreFactory}.
     *
     * @return the {@link StoreFactory}
     */
    public StoreFactory getStoreFactory() {
        if (storeFactory != null) return storeFactory;
        storeFactory = keycloakSession.getProvider(CachedStoreFactoryProvider.class);
        if (storeFactory == null) storeFactory = getLocalStoreFactory();
        storeFactory = createStoreFactory(storeFactory);
        return storeFactory;
    }

    /**
     * No cache sits in front of this
     *
     * @return
     */
    public StoreFactory getLocalStoreFactory() {
        if (storeFactoryDelegate != null) return storeFactoryDelegate;
        storeFactoryDelegate = keycloakSession.getProvider(StoreFactory.class);
        return storeFactoryDelegate;
    }

    private StoreFactory createStoreFactory(StoreFactory storeFactory) {
        return new StoreFactory() {
            @Override
            public ResourceStore getResourceStore() {
                return storeFactory.getResourceStore();
            }

            @Override
            public ResourceServerStore getResourceServerStore() {
                return storeFactory.getResourceServerStore();
            }

            @Override
            public ScopeStore getScopeStore() {
                return storeFactory.getScopeStore();
            }

            @Override
            public PolicyStore getPolicyStore() {
                PolicyStore policyStore = storeFactory.getPolicyStore();
                return new PolicyStore() {
                    @Override
                    public Policy create(AbstractPolicyRepresentation representation, ResourceServer resourceServer) {
                        Set<String> resources = representation.getResources();

                        if (resources != null) {
                            representation.setResources(resources.stream().map(id -> {
                                Resource resource = getResourceStore().findById(id, resourceServer.getId());

                                if (resource == null) {
                                    resource = getResourceStore().findByName(id, resourceServer.getId());
                                }

                                if (resource == null) {
                                    throw new RuntimeException("Resource [" + id + "] does not exist");
                                }

                                return resource.getId();
                            }).collect(Collectors.toSet()));
                        }

                        Set<String> scopes = representation.getScopes();

                        if (scopes != null) {
                            representation.setScopes(scopes.stream().map(id -> {
                                Scope scope = getScopeStore().findById(id, resourceServer.getId());

                                if (scope == null) {
                                    scope = getScopeStore().findByName(id, resourceServer.getId());
                                }

                                if (scope == null) {
                                    throw new RuntimeException("Scope [" + id + "] does not exist");
                                }

                                return scope.getId();
                            }).collect(Collectors.toSet()));
                        }


                        Set<String> policies = representation.getPolicies();

                        if (policies != null) {
                            representation.setPolicies(policies.stream().map(id -> {
                                Policy policy = getPolicyStore().findById(id, resourceServer.getId());

                                if (policy == null) {
                                    policy = getPolicyStore().findByName(id, resourceServer.getId());
                                }

                                if (policy == null) {
                                    throw new RuntimeException("Policy [" + id + "] does not exist");
                                }

                                return policy.getId();
                            }).collect(Collectors.toSet()));
                        }

                        return RepresentationToModel.toModel(representation, AuthorizationProvider.this, policyStore.create(representation, resourceServer));
                    }

                    @Override
                    public void delete(String id) {
                        Policy policy = findById(id, null);

                        if (policy != null) {
                            ResourceServer resourceServer = policy.getResourceServer();

                            findDependentPolicies(policy.getId(), resourceServer.getId()).forEach(dependentPolicy -> {
                                dependentPolicy.removeAssociatedPolicy(policy);
                                if (dependentPolicy.getAssociatedPolicies().isEmpty()) {
                                    delete(dependentPolicy.getId());
                                }
                            });

                            policyStore.delete(id);
                        }
                    }

                    @Override
                    public Policy findById(String id, String resourceServerId) {
                        return policyStore.findById(id, resourceServerId);
                    }

                    @Override
                    public Policy findByName(String name, String resourceServerId) {
                        return policyStore.findByName(name, resourceServerId);
                    }

                    @Override
                    public List<Policy> findByResourceServer(String resourceServerId) {
                        return policyStore.findByResourceServer(resourceServerId);
                    }

                    @Override
                    public List<Policy> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
                        return policyStore.findByResourceServer(attributes, resourceServerId, firstResult, maxResult);
                    }

                    @Override
                    public List<Policy> findByResource(String resourceId, String resourceServerId) {
                        return policyStore.findByResource(resourceId, resourceServerId);
                    }

                    @Override
                    public List<Policy> findByResourceType(String resourceType, String resourceServerId) {
                        return policyStore.findByResourceType(resourceType, resourceServerId);
                    }

                    @Override
                    public List<Policy> findByScopeIds(List<String> scopeIds, String resourceServerId) {
                        return policyStore.findByScopeIds(scopeIds, resourceServerId);
                    }

                    @Override
                    public List<Policy> findByType(String type, String resourceServerId) {
                        return policyStore.findByType(type, resourceServerId);
                    }

                    @Override
                    public List<Policy> findDependentPolicies(String id, String resourceServerId) {
                        return policyStore.findDependentPolicies(id, resourceServerId);
                    }
                };
            }

            @Override
            public void close() {
                storeFactory.close();
            }
        };
    }

    /**
     * Returns the registered {@link PolicyProviderFactory}.
     *
     * @return a {@link List} containing all registered {@link PolicyProviderFactory}
     */
    public Collection<PolicyProviderFactory> getProviderFactories() {
        return this.policyProviderFactories.values();
    }

    /**
     * Returns a {@link PolicyProviderFactory} given a <code>type</code>.
     *
     * @param type the type of the policy provider
     * @param <F> the expected type of the provider
     * @return a {@link PolicyProviderFactory} with the given <code>type</code>
     */
    public <F extends PolicyProviderFactory> F getProviderFactory(String type) {
        return (F) policyProviderFactories.get(type);
    }

    /**
     * Returns a {@link PolicyProviderFactory} given a <code>type</code>.
     *
     * @param type the type of the policy provider
     * @param <P> the expected type of the provider
     * @return a {@link PolicyProvider} with the given <code>type</code>
     */
    public <P extends PolicyProvider> P getProvider(String type) {
        PolicyProviderFactory policyProviderFactory = policyProviderFactories.get(type);

        if (policyProviderFactory == null) {
            return null;
        }

        return (P) policyProviderFactory.create(this);
    }

    public KeycloakSession getKeycloakSession() {
        return this.keycloakSession;
    }

    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public void close() {

    }
}
