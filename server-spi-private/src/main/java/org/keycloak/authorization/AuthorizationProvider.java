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
     * @param <F> the expected type of the provider
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
            public void delete(String id) {
                Scope scope = findById(null, id);
                PermissionTicketStore ticketStore = AuthorizationProvider.this.getStoreFactory().getPermissionTicketStore();
                List<PermissionTicket> permissions = ticketStore.findByScope(scope.getResourceServer(), scope);

                for (PermissionTicket permission : permissions) {
                    ticketStore.delete(permission.getId());
                }

                delegate.delete(id);
            }

            @Override
            public Scope findById(String resourceServerId, String id) {
                return delegate.findById(resourceServerId, id);
            }

            @Override
            public Scope findByName(String resourceServerId, String name) {
                return delegate.findByName(resourceServerId, name);
            }

            @Override
            public List<Scope> findByResourceServer(String id) {
                return delegate.findByResourceServer(id);
            }

            @Override
            public List<Scope> findByResourceServer(String resourceServerId, Map<Scope.FilterOption, String[]> attributes, int firstResult, int maxResult) {
                return delegate.findByResourceServer(resourceServerId, attributes, firstResult, maxResult);
            }
        };
    }

    private PolicyStore createPolicyWrapper(StoreFactory storeFactory) {
        return new PolicyStore() {

            PolicyStore policyStore = storeFactory.getPolicyStore();

            @Override
            public Policy create(ResourceServer resourceServer, AbstractPolicyRepresentation representation) {
                Set<String> resources = representation.getResources();

                if (resources != null) {
                    representation.setResources(resources.stream().map(id -> {
                        Resource resource = storeFactory.getResourceStore().findById(resourceServer.getId(), id);

                        if (resource == null) {
                            resource = storeFactory.getResourceStore().findByName(resourceServer.getId(), id);
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
                        Scope scope = storeFactory.getScopeStore().findById(resourceServer.getId(), id);

                        if (scope == null) {
                            scope = storeFactory.getScopeStore().findByName(resourceServer.getId(), id);
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
                        Policy policy = storeFactory.getPolicyStore().findById(resourceServer.getId(), id);

                        if (policy == null) {
                            policy = storeFactory.getPolicyStore().findByName(resourceServer.getId(), id);
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
            public void delete(String id) {
                Policy policy = findById(null, id);

                if (policy != null) {
                    ResourceServer resourceServer = policy.getResourceServer();

                    // if uma policy (owned by a user) also remove associated policies
                    if (policy.getOwner() != null) {
                        for (Policy associatedPolicy : policy.getAssociatedPolicies()) {
                            // only remove associated policies created from the policy being deleted
                            if (associatedPolicy.getOwner() != null) {
                                policy.removeAssociatedPolicy(associatedPolicy);
                                policyStore.delete(associatedPolicy.getId());
                            }
                        }
                    }

                    findDependentPolicies(resourceServer.getId(), policy.getId()).forEach(dependentPolicy -> {
                        dependentPolicy.removeAssociatedPolicy(policy);
                        if (dependentPolicy.getAssociatedPolicies().isEmpty()) {
                            delete(dependentPolicy.getId());
                        }
                    });

                    policyStore.delete(id);
                }
            }

            @Override
            public Policy findById(String resourceServerId, String id) {
                return policyStore.findById(resourceServerId, id);
            }

            @Override
            public Policy findByName(String resourceServerId, String name) {
                return policyStore.findByName(resourceServerId, name);
            }

            @Override
            public List<Policy> findByResourceServer(String resourceServerId) {
                return policyStore.findByResourceServer(resourceServerId);
            }

            @Override
            public List<Policy> findByResourceServer(String resourceServerId, Map<Policy.FilterOption, String[]> attributes, int firstResult, int maxResult) {
                return policyStore.findByResourceServer(resourceServerId, attributes, firstResult, maxResult);
            }

            @Override
            public List<Policy> findByResource(String resourceServerId, String resourceId) {
                return policyStore.findByResource(resourceServerId, resourceId);
            }

            @Override
            public void findByResource(String resourceServerId, String resourceId, Consumer<Policy> consumer) {
                policyStore.findByResource(resourceServerId, resourceId, consumer);
            }

            @Override
            public List<Policy> findByResourceType(String resourceServerId, String resourceType) {
                return policyStore.findByResourceType(resourceServerId, resourceType);
            }

            @Override
            public List<Policy> findByScopeIds(String resourceServerId, List<String> scopeIds) {
                return policyStore.findByScopeIds(resourceServerId, scopeIds);
            }

            @Override
            public List<Policy> findByScopeIds(String resourceServerId, String resourceId, List<String> scopeIds) {
                return policyStore.findByScopeIds(resourceServerId, resourceId, scopeIds);
            }

            @Override
            public void findByScopeIds(String resourceServerId, String resourceId, List<String> scopeIds, Consumer<Policy> consumer) {
                policyStore.findByScopeIds(resourceServerId, resourceId, scopeIds, consumer);
            }

            @Override
            public List<Policy> findByType(String resourceServerId, String type) {
                return policyStore.findByType(resourceServerId, type);
            }

            @Override
            public List<Policy> findDependentPolicies(String resourceServerId, String id) {
                return policyStore.findDependentPolicies(resourceServerId, id);
            }

            @Override
            public void findByResourceType(String id, String type, Consumer<Policy> policyConsumer) {
                policyStore.findByResourceType(id, type, policyConsumer);
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
            public void delete(String id) {
                Resource resource = findById(null, id);
                StoreFactory storeFactory = AuthorizationProvider.this.getStoreFactory();
                PermissionTicketStore ticketStore = storeFactory.getPermissionTicketStore();
                List<PermissionTicket> permissions = ticketStore.findByResource(resource.getResourceServer(), resource);

                for (PermissionTicket permission : permissions) {
                    ticketStore.delete(permission.getId());
                }

                PolicyStore policyStore = storeFactory.getPolicyStore();
                List<Policy> policies = policyStore.findByResource(resource.getResourceServer().getId(), id);

                for (Policy policyModel : policies) {
                    if (policyModel.getResources().size() == 1) {
                        policyStore.delete(policyModel.getId());
                    } else {
                        policyModel.removeResource(resource);
                    }
                }

                delegate.delete(id);
            }

            @Override
            public Resource findById(String resourceServerId, String id) {
                return delegate.findById(resourceServerId, id);
            }

            @Override
            public List<Resource> findByOwner(String resourceServerId, String ownerId) {
                return delegate.findByOwner(resourceServerId, ownerId);
            }

            @Override
            public void findByOwner(String resourceServerId, String ownerId, Consumer<Resource> consumer) {
                delegate.findByOwner(resourceServerId, ownerId, consumer);
            }

            @Override
            public List<Resource> findByOwner(String resourceServerId, String ownerId, int first, int max) {
                return delegate.findByOwner(resourceServerId, ownerId, first, max);
            }

            @Override
            public List<Resource> findByUri(String resourceServerId, String uri) {
                return delegate.findByUri(resourceServerId, uri);
            }

            @Override
            public List<Resource> findByResourceServer(String resourceServerId) {
                return delegate.findByResourceServer(resourceServerId);
            }

            @Override
            public List<Resource> findByResourceServer(String resourceServerId, Map<Resource.FilterOption, String[]> attributes, int firstResult, int maxResult) {
                return delegate.findByResourceServer(resourceServerId, attributes, firstResult, maxResult);
            }

            @Override
            public List<Resource> findByScope(String resourceServerId, List<String> id) {
                return delegate.findByScope(resourceServerId, id);
            }

            @Override
            public void findByScope(String resourceServerId, List<String> scopes, Consumer<Resource> consumer) {
                delegate.findByScope(resourceServerId, scopes, consumer);
            }

            @Override
            public Resource findByName(String name, String resourceServerId) {
                return delegate.findByName(name, resourceServerId);
            }

            @Override
            public Resource findByName(String resourceServerId, String name, String ownerId) {
                return delegate.findByName(resourceServerId, name, ownerId);
            }

            @Override
            public List<Resource> findByType(String resourceServerId, String type) {
                return delegate.findByType(resourceServerId, type);
            }

            @Override
            public void findByType(String resourceServerId, String type, Consumer<Resource> consumer) {
                delegate.findByType(resourceServerId, type, consumer);
            }

            @Override
            public void findByType(String resourceServerId, String type, String owner, Consumer<Resource> consumer) {
                delegate.findByType(resourceServerId, type, owner, consumer);
            }

            @Override
            public List<Resource> findByType(String resourceServerId, String type, String owner) {
                return delegate.findByType(resourceServerId, type);
            }

            @Override
            public List<Resource> findByTypeInstance(String type, String resourceServerId) {
                return delegate.findByTypeInstance(type, resourceServerId);
            }

            @Override
            public void findByTypeInstance(String type, String resourceServerId, Consumer<Resource> consumer) {
                delegate.findByTypeInstance(resourceServerId, type, consumer);
            }
        };
    }
}
