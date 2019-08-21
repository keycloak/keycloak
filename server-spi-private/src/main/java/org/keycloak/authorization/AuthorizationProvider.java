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
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
     * @return a {@link List} containing all registered {@link PolicyProviderFactory}
     */
    public Collection<PolicyProviderFactory> getProviderFactories() {
        return keycloakSession.getKeycloakSessionFactory().getProviderFactories(PolicyProvider.class).stream().map(
                PolicyProviderFactory.class::cast).collect(Collectors.toList());
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
            public Scope create(String name, ResourceServer resourceServer) {
                return delegate.create(name, resourceServer);
            }

            @Override
            public Scope create(String id, String name, ResourceServer resourceServer) {
                return delegate.create(id, name, resourceServer);
            }

            @Override
            public void delete(String id) {
                Scope scope = findById(id, null);
                PermissionTicketStore ticketStore = AuthorizationProvider.this.getStoreFactory().getPermissionTicketStore();
                List<PermissionTicket> permissions = ticketStore.findByScope(id, scope.getResourceServer().getId());

                for (PermissionTicket permission : permissions) {
                    ticketStore.delete(permission.getId());
                }

                delegate.delete(id);
            }

            @Override
            public Scope findById(String id, String resourceServerId) {
                return delegate.findById(id, resourceServerId);
            }

            @Override
            public Scope findByName(String name, String resourceServerId) {
                return delegate.findByName(name, resourceServerId);
            }

            @Override
            public List<Scope> findByResourceServer(String id) {
                return delegate.findByResourceServer(id);
            }

            @Override
            public List<Scope> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
                return delegate.findByResourceServer(attributes, resourceServerId, firstResult, maxResult);
            }
        };
    }

    private PolicyStore createPolicyWrapper(StoreFactory storeFactory) {
        return new PolicyStore() {

            PolicyStore policyStore = storeFactory.getPolicyStore();

            @Override
            public Policy create(AbstractPolicyRepresentation representation, ResourceServer resourceServer) {
                Set<String> resources = representation.getResources();

                if (resources != null) {
                    representation.setResources(resources.stream().map(id -> {
                        Resource resource = storeFactory.getResourceStore().findById(id, resourceServer.getId());

                        if (resource == null) {
                            resource = storeFactory.getResourceStore().findByName(id, resourceServer.getId());
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
                        Scope scope = storeFactory.getScopeStore().findById(id, resourceServer.getId());

                        if (scope == null) {
                            scope = storeFactory.getScopeStore().findByName(id, resourceServer.getId());
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
                        Policy policy = storeFactory.getPolicyStore().findById(id, resourceServer.getId());

                        if (policy == null) {
                            policy = storeFactory.getPolicyStore().findByName(id, resourceServer.getId());
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
            public void findByResource(String resourceId, String resourceServerId, Consumer<Policy> consumer) {
                policyStore.findByResource(resourceId, resourceServerId, consumer);
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
            public List<Policy> findByScopeIds(List<String> scopeIds, String resourceId, String resourceServerId) {
                return policyStore.findByScopeIds(scopeIds, resourceId, resourceServerId);
            }

            @Override
            public void findByScopeIds(List<String> scopeIds, String resourceId, String resourceServerId, Consumer<Policy> consumer) {
                policyStore.findByScopeIds(scopeIds, resourceId, resourceServerId, consumer);
            }

            @Override
            public List<Policy> findByType(String type, String resourceServerId) {
                return policyStore.findByType(type, resourceServerId);
            }

            @Override
            public List<Policy> findDependentPolicies(String id, String resourceServerId) {
                return policyStore.findDependentPolicies(id, resourceServerId);
            }

            @Override
            public void findByResourceType(String type, String id, Consumer<Policy> policyConsumer) {
                policyStore.findByResourceType(type, id, policyConsumer);
            }
        };
    }

    private ResourceStore createResourceStoreWrapper(StoreFactory storeFactory) {
        return new ResourceStore() {
            ResourceStore delegate = storeFactory.getResourceStore();

            @Override
            public Resource create(String name, ResourceServer resourceServer, String owner) {
                return delegate.create(name, resourceServer, owner);
            }

            @Override
            public Resource create(String id, String name, ResourceServer resourceServer, String owner) {
                return delegate.create(id, name, resourceServer, owner);
            }

            @Override
            public void delete(String id) {
                Resource resource = findById(id, null);
                StoreFactory storeFactory = AuthorizationProvider.this.getStoreFactory();
                PermissionTicketStore ticketStore = storeFactory.getPermissionTicketStore();
                List<PermissionTicket> permissions = ticketStore.findByResource(id, resource.getResourceServer().getId());

                for (PermissionTicket permission : permissions) {
                    ticketStore.delete(permission.getId());
                }

                PolicyStore policyStore = storeFactory.getPolicyStore();
                List<Policy> policies = policyStore.findByResource(id, resource.getResourceServer().getId());

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
            public Resource findById(String id, String resourceServerId) {
                return delegate.findById(id, resourceServerId);
            }

            @Override
            public List<Resource> findByOwner(String ownerId, String resourceServerId) {
                return delegate.findByOwner(ownerId, resourceServerId);
            }

            @Override
            public void findByOwner(String ownerId, String resourceServerId, Consumer<Resource> consumer) {
                delegate.findByOwner(ownerId, resourceServerId, consumer);
            }

            @Override
            public List<Resource> findByOwner(String ownerId, String resourceServerId, int first, int max) {
                return delegate.findByOwner(ownerId, resourceServerId, first, max);
            }

            @Override
            public List<Resource> findByUri(String uri, String resourceServerId) {
                return delegate.findByUri(uri, resourceServerId);
            }

            @Override
            public List<Resource> findByResourceServer(String resourceServerId) {
                return delegate.findByResourceServer(resourceServerId);
            }

            @Override
            public List<Resource> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
                return delegate.findByResourceServer(attributes, resourceServerId, firstResult, maxResult);
            }

            @Override
            public List<Resource> findByScope(List<String> id, String resourceServerId) {
                return delegate.findByScope(id, resourceServerId);
            }

            @Override
            public void findByScope(List<String> scopes, String resourceServerId, Consumer<Resource> consumer) {
                delegate.findByScope(scopes, resourceServerId, consumer);
            }

            @Override
            public Resource findByName(String name, String resourceServerId) {
                return delegate.findByName(name, resourceServerId);
            }

            @Override
            public Resource findByName(String name, String ownerId, String resourceServerId) {
                return delegate.findByName(name, ownerId, resourceServerId);
            }

            @Override
            public List<Resource> findByType(String type, String resourceServerId) {
                return delegate.findByType(type, resourceServerId);
            }

            @Override
            public void findByType(String type, String resourceServerId, Consumer<Resource> consumer) {
                delegate.findByType(type, resourceServerId, consumer);
            }

            @Override
            public void findByType(String type, String owner, String resourceServerId, Consumer<Resource> consumer) {
                delegate.findByType(type, owner, resourceServerId, consumer);
            }

            @Override
            public List<Resource> findByType(String type, String owner, String resourceServerId) {
                return delegate.findByType(type, resourceServerId);
            }

            @Override
            public List<Resource> findByTypeInstance(String type, String resourceServerId) {
                return delegate.findByTypeInstance(type, resourceServerId);
            }

            @Override
            public void findByTypeInstance(String type, String resourceServerId, Consumer<Resource> consumer) {
                delegate.findByTypeInstance(type, resourceServerId, consumer);
            }
        };
    }
}
