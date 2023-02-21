/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authorization;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.evaluator.Evaluators;
import org.keycloak.authorization.policy.evaluation.PolicyEvaluator;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PermissionTicketStore;
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

    private final PolicyEvaluator policyEvaluator;
    private StoreFactory storeFactory;
    private StoreFactory storeFactoryDelegate;
    private final KeycloakSession keycloakSession;
    private final RealmModel realm;

    public AuthorizationProvider(KeycloakSession session, RealmModel realm, PolicyEvaluator policyEvaluator) {
        this.keycloakSession = session;
        this.realm = realm;
        this.policyEvaluator = policyEvaluator;
    }

    /**
     * Returns a {@link Evaluators} instance from where {@link org.keycloak.authorization.policy.evaluation.PolicyEvaluator} instances
     * can be obtained.
     *
     * @return a {@link Evaluators} instance
     */
    public Evaluators evaluators() {
        return new Evaluators(this);
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

    /**
     * Returns the registered {@link PolicyProviderFactory}.
     *
     * @return a {@link Stream} containing all registered {@link PolicyProviderFactory}
     */
    public Stream<PolicyProviderFactory> getProviderFactoriesStream() {
        return keycloakSession.getKeycloakSessionFactory().getProviderFactoriesStream(PolicyProvider.class)
                .map(PolicyProviderFactory.class::cast);
    }

    /**
     * Returns a {@link PolicyProviderFactory} given a <code>type</code>.
     *
     * @param type the type of the policy provider
     * @return a {@link PolicyProviderFactory} with the given <code>type</code>
     */
    public PolicyProviderFactory getProviderFactory(String type) {
        return (PolicyProviderFactory) keycloakSession.getKeycloakSessionFactory().getProviderFactory(PolicyProvider.class, type);
    }

    /**
     * Returns a {@link PolicyProviderFactory} given a <code>type</code>.
     *
     * @param type the type of the policy provider
     * @param <P> the expected type of the provider
     * @return a {@link PolicyProvider} with the given <code>type</code>
     */
    public <P extends PolicyProvider> P getProvider(String type) {
        PolicyProviderFactory policyProviderFactory = getProviderFactory(type);

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

    public PolicyEvaluator getPolicyEvaluator() {
        return policyEvaluator;
    }

    @Override
    public void close() {

    }

    private StoreFactory createStoreFactory(StoreFactory storeFactory) {
        return new StoreFactory() {

            ResourceStore resourceStore;
            ScopeStore scopeStore;
            PolicyStore policyStore;

            @Override
            public ResourceStore getResourceStore() {
                if (resourceStore == null) {
                    resourceStore = createResourceStoreWrapper(storeFactory);
                }
                return resourceStore;
            }

            @Override
            public ResourceServerStore getResourceServerStore() {
                return storeFactory.getResourceServerStore();
            }

            @Override
            public ScopeStore getScopeStore() {
                if (scopeStore == null) {
                    scopeStore = createScopeWrapper(storeFactory);
                }
                return scopeStore;
            }

            @Override
            public PolicyStore getPolicyStore() {
                if (policyStore == null) {
                    policyStore = createPolicyWrapper(storeFactory);
                }
                return policyStore;
            }

            @Override
            public PermissionTicketStore getPermissionTicketStore() {
                return storeFactory.getPermissionTicketStore();
            }

            @Override
            public void close() {
                storeFactory.close();
            }

            @Override
            public void setReadOnly(boolean readOnly) {
                storeFactory.setReadOnly(readOnly);
            }

            @Override
            public boolean isReadOnly() {
                return storeFactory.isReadOnly();
            }
        };
    }

    private ScopeStore createScopeWrapper(StoreFactory storeFactory) {
        return new ScopeStore() {

            ScopeStore delegate = storeFactory.getScopeStore();

            @Override
            public Scope create(ResourceServer resourceServer, String name) {
                return delegate.create(resourceServer, name);
            }

            @Override
            public Scope create(ResourceServer resourceServer, String id, String name) {
                return delegate.create(resourceServer, id, name);
            }

            @Override
            public void delete(RealmModel realm, String id) {
                Scope scope = findById(realm, null, id);
                PermissionTicketStore ticketStore = AuthorizationProvider.this.getStoreFactory().getPermissionTicketStore();
                List<PermissionTicket> permissions = ticketStore.findByScope(scope.getResourceServer(), scope);

                for (PermissionTicket permission : permissions) {
                    ticketStore.delete(realm, permission.getId());
                }

                delegate.delete(realm, id);
            }

            @Override
            public Scope findById(RealmModel realm, ResourceServer resourceServer, String id) {
                return delegate.findById(realm, resourceServer, id);
            }

            @Override
            public Scope findByName(ResourceServer resourceServer, String name) {
                return delegate.findByName(resourceServer, name);
            }

            @Override
            public List<Scope> findByResourceServer(ResourceServer resourceServer) {
                return delegate.findByResourceServer(resourceServer);
            }

            @Override
            public List<Scope> findByResourceServer(ResourceServer resourceServer, Map<Scope.FilterOption, String[]> attributes, Integer firstResult, Integer maxResults) {
                return delegate.findByResourceServer(resourceServer, attributes, firstResult, maxResults);
            }
        };
    }

    private PolicyStore createPolicyWrapper(StoreFactory storeFactory) {
        return new PolicyStore() {

            PolicyStore policyStore = storeFactory.getPolicyStore();

            @Override
            public Policy create(ResourceServer resourceServer, AbstractPolicyRepresentation representation) {
                Set<String> resources = representation.getResources();
                RealmModel realm = resourceServer.getRealm();

                if (resources != null) {
                    representation.setResources(resources.stream().map(id -> {
                        Resource resource = storeFactory.getResourceStore().findById(realm, resourceServer, id);

                        if (resource == null) {
                            resource = storeFactory.getResourceStore().findByName(resourceServer, id);
                        }

                        if (resource == null) {
                            throw new RuntimeException("Resource [" + id + "] does not exist or is not owned by the resource server.");
                        }

                        return resource.getId();
                    }).collect(Collectors.toSet()));
                }

                Set<String> scopes = representation.getScopes();

                if (scopes != null) {
                    representation.setScopes(scopes.stream().map(id -> {
                        Scope scope = storeFactory.getScopeStore().findById(realm, resourceServer, id);

                        if (scope == null) {
                            scope = storeFactory.getScopeStore().findByName(resourceServer, id);
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
                        Policy policy = storeFactory.getPolicyStore().findById(realm, resourceServer, id);

                        if (policy == null) {
                            policy = storeFactory.getPolicyStore().findByName(resourceServer, id);
                        }

                        if (policy == null) {
                            throw new RuntimeException("Policy [" + id + "] does not exist");
                        }

                        return policy.getId();
                    }).collect(Collectors.toSet()));
                }

                return RepresentationToModel.toModel(representation, AuthorizationProvider.this, policyStore.create(resourceServer, representation));
            }

            @Override
            public void delete(RealmModel realm, String id) {
                Policy policy = findById(realm, null, id);

                if (policy != null) {
                    ResourceServer resourceServer = policy.getResourceServer();

                    // if uma policy (owned by a user) also remove associated policies
                    if (policy.getOwner() != null) {
                        for (Policy associatedPolicy : policy.getAssociatedPolicies()) {
                            // only remove associated policies created from the policy being deleted
                            if (associatedPolicy.getOwner() != null) {
                                policy.removeAssociatedPolicy(associatedPolicy);
                                policyStore.delete(realm, associatedPolicy.getId());
                            }
                        }
                    }

                    findDependentPolicies(resourceServer, policy.getId()).forEach(dependentPolicy -> {
                        dependentPolicy.removeAssociatedPolicy(policy);
                        if (dependentPolicy.getAssociatedPolicies().isEmpty()) {
                            delete(realm, dependentPolicy.getId());
                        }
                    });

                    policyStore.delete(realm, id);
                }
            }

            @Override
            public Policy findById(RealmModel realm, ResourceServer resourceServer, String id) {
                return policyStore.findById(realm, resourceServer, id);
            }

            @Override
            public Policy findByName(ResourceServer resourceServer, String name) {
                return policyStore.findByName(resourceServer, name);
            }

            @Override
            public List<Policy> findByResourceServer(ResourceServer resourceServer) {
                return policyStore.findByResourceServer(resourceServer);
            }

            @Override
            public List<Policy> find(RealmModel realm, ResourceServer resourceServer, Map<Policy.FilterOption, String[]> attributes, Integer firstResult, Integer maxResults) {
                return policyStore.find(realm, resourceServer, attributes, firstResult, maxResults);
            }

            @Override
            public List<Policy> findByResource(ResourceServer resourceServer, Resource resource) {
                return policyStore.findByResource(resourceServer, resource);
            }

            @Override
            public void findByResource(ResourceServer resourceServer, Resource resource, Consumer<Policy> consumer) {
                policyStore.findByResource(resourceServer, resource, consumer);
            }

            @Override
            public List<Policy> findByResourceType(ResourceServer resourceServer, String resourceType) {
                return policyStore.findByResourceType(resourceServer, resourceType);
            }

            @Override
            public List<Policy> findByScopes(ResourceServer resourceServer, List<Scope> scopes) {
                return policyStore.findByScopes(resourceServer, scopes);
            }

            @Override
            public List<Policy> findByScopes(ResourceServer resourceServer, Resource resource, List<Scope> scopes) {
                return policyStore.findByScopes(resourceServer, resource, scopes);
            }

            @Override
            public void findByScopes(ResourceServer resourceServer, Resource resource, List<Scope> scopes, Consumer<Policy> consumer) {
                policyStore.findByScopes(resourceServer, resource, scopes, consumer);
            }

            @Override
            public List<Policy> findByType(ResourceServer resourceServer, String type) {
                return policyStore.findByType(resourceServer, type);
            }

            @Override
            public List<Policy> findDependentPolicies(ResourceServer resourceServer, String id) {
                return policyStore.findDependentPolicies(resourceServer, id);
            }

            @Override
            public void findByResourceType(ResourceServer resourceServer, String type, Consumer<Policy> policyConsumer) {
                policyStore.findByResourceType(resourceServer, type, policyConsumer);
            }
        };
    }

    private ResourceStore createResourceStoreWrapper(StoreFactory storeFactory) {
        return new ResourceStore() {
            ResourceStore delegate = storeFactory.getResourceStore();

            @Override
            public Resource create(ResourceServer resourceServer, String name, String owner) {
                return delegate.create(resourceServer, name, owner);
            }

            @Override
            public Resource create(ResourceServer resourceServer, String id, String name, String owner) {
                return delegate.create(resourceServer, id, name, owner);
            }

            @Override
            public void delete(RealmModel realm, String id) {
                Resource resource = findById(realm, null, id);
                StoreFactory storeFactory = AuthorizationProvider.this.getStoreFactory();
                PermissionTicketStore ticketStore = storeFactory.getPermissionTicketStore();
                List<PermissionTicket> permissions = ticketStore.findByResource(resource.getResourceServer(), resource);

                for (PermissionTicket permission : permissions) {
                    ticketStore.delete(realm, permission.getId());
                }

                PolicyStore policyStore = storeFactory.getPolicyStore();
                List<Policy> policies = policyStore.findByResource(resource.getResourceServer(), resource);

                for (Policy policyModel : policies) {
                    if (policyModel.getResources().size() == 1) {
                        policyStore.delete(realm, policyModel.getId());
                    } else {
                        policyModel.removeResource(resource);
                    }
                }

                delegate.delete(realm, id);
            }

            @Override
            public Resource findById(RealmModel realm, ResourceServer resourceServer, String id) {
                return delegate.findById(realm, resourceServer, id);
            }

            @Override
            public List<Resource> findByOwner(RealmModel realm, ResourceServer resourceServer, String ownerId) {
                return delegate.findByOwner(realm, resourceServer, ownerId);
            }

            @Override
            public void findByOwner(RealmModel realm, ResourceServer resourceServer, String ownerId, Consumer<Resource> consumer) {
                delegate.findByOwner(realm, resourceServer, ownerId, consumer);
            }

            @Override
            public List<Resource> findByResourceServer(ResourceServer resourceServer) {
                return delegate.findByResourceServer(resourceServer);
            }

            @Override
            public List<Resource> find(RealmModel realm, ResourceServer resourceServer, Map<Resource.FilterOption, String[]> attributes, Integer firstResult, Integer maxResults) {
                return delegate.find(realm, resourceServer, attributes, firstResult, maxResults);
            }

            @Override
            public List<Resource> findByScopes(ResourceServer resourceServer, Set<Scope> scopes) {
                return delegate.findByScopes(resourceServer, scopes);
            }

            @Override
            public void findByScopes(ResourceServer resourceServer, Set<Scope> scopes, Consumer<Resource> consumer) {
                delegate.findByScopes(resourceServer, scopes, consumer);
            }

            @Override
            public Resource findByName(ResourceServer resourceServer, String name, String ownerId) {
                return delegate.findByName(resourceServer, name, ownerId);
            }

            @Override
            public List<Resource> findByType(ResourceServer resourceServer, String type) {
                return delegate.findByType(resourceServer, type);
            }

            @Override
            public void findByType(ResourceServer resourceServer, String type, Consumer<Resource> consumer) {
                delegate.findByType(resourceServer, type, consumer);
            }

            @Override
            public void findByType(ResourceServer resourceServer, String type, String owner, Consumer<Resource> consumer) {
                delegate.findByType(resourceServer, type, owner, consumer);
            }

            @Override
            public void findByTypeInstance(ResourceServer resourceServer, String type, Consumer<Resource> consumer) {
                delegate.findByTypeInstance(resourceServer, type, consumer);
            }
        };
    }
}
