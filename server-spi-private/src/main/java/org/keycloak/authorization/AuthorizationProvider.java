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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
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

    public PolicyEvaluator getPolicyEvaluator(ResourceServer resourceServer) {
        PolicyEvaluator schemaPolicyEvaluator = AdminPermissionsSchema.SCHEMA.getPolicyEvaluator(keycloakSession, resourceServer);
        return schemaPolicyEvaluator == null ? policyEvaluator : schemaPolicyEvaluator;
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
            public Scope findById(ResourceServer resourceServer, String id) {
                return delegate.findById(resourceServer, id);
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
                AdminPermissionsSchema.SCHEMA.throwExceptionIfResourceTypeOrScopesNotProvided(keycloakSession, resourceServer, representation);
                Set<String> resources = representation.getResources();

                if (resources != null && !resources.isEmpty()) {
                    representation.setResources(resources.stream().map(id -> {
                        Resource resource = AdminPermissionsSchema.SCHEMA.getOrCreateResource(keycloakSession, resourceServer, representation.getType(), representation.getResourceType(), id);

                        if (resource == null) {
                            resource = storeFactory.getResourceStore().findById(resourceServer, id);

                            if (resource == null) {
                                resource = storeFactory.getResourceStore().findByName(resourceServer, id);
                            }

                            if (resource == null) {
                                throw new RuntimeException("Resource [" + id + "] does not exist or is not owned by the resource server.");
                            }

                            return resource.getId();
                        }

                        return Optional.ofNullable(resource).map(Resource::getId).orElse(null);
                    }).filter(Objects::nonNull).collect(Collectors.toSet()));
                }

                Set<String> scopes = representation.getScopes();

                if (scopes != null) {
                    representation.setScopes(scopes.stream()
                        .map(id -> AdminPermissionsSchema.SCHEMA.getScope(keycloakSession, resourceServer, representation.getResourceType(), id).getId())
                        .collect(Collectors.toSet()));
                }

                Set<String> policies = representation.getPolicies();

                if (policies != null) {
                    representation.setPolicies(policies.stream().map(id -> {
                        Policy policy = storeFactory.getPolicyStore().findById(resourceServer, id);

                        if (policy == null) {
                            policy = storeFactory.getPolicyStore().findByName(resourceServer, id);
                        }

                        if (policy == null) {
                            throw new RuntimeException("Policy [" + id + "] does not exist");
                        }

                        return policy.getId();
                    }).collect(Collectors.toSet()));
                }

                Policy policy = RepresentationToModel.toModel(representation, AuthorizationProvider.this, policyStore.create(resourceServer, representation));

                AdminPermissionsSchema.SCHEMA.addUResourceTypeResource(keycloakSession, resourceServer, policy, representation.getResourceType());

                return policy;
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

                    findDependentPolicies(resourceServer, policy.getId()).forEach(dependentPolicy -> {
                        dependentPolicy.removeAssociatedPolicy(policy);
                        if (dependentPolicy.getAssociatedPolicies().isEmpty()) {
                            delete(dependentPolicy.getId());
                        }
                    });

                    policyStore.delete(id);
                }
            }

            @Override
            public Policy findById(ResourceServer resourceServer, String id) {
                return policyStore.findById(resourceServer, id);
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
            public List<Policy> find(ResourceServer resourceServer, Map<Policy.FilterOption, String[]> attributes, Integer firstResult, Integer maxResults) {
                return policyStore.find(resourceServer, attributes, firstResult, maxResults);
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
            public Stream<Policy> findDependentPolicies(ResourceServer resourceServer, String resourceType, String associatedPolicyType, String configKey, String configValue) {
                return policyStore.findDependentPolicies(resourceServer, resourceType, associatedPolicyType, configKey, configValue);
            }

            @Override
            public Stream<Policy> findDependentPolicies(ResourceServer resourceServer, String resourceType, String associatedPolicyType, String configKey, List<String> configValues) {
                return policyStore.findDependentPolicies(resourceServer, resourceType, associatedPolicyType, configKey, configValues);
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
            public void delete(String id) {
                Resource resource = findById(null, id);
                StoreFactory storeFactory = AuthorizationProvider.this.getStoreFactory();
                PermissionTicketStore ticketStore = storeFactory.getPermissionTicketStore();
                ResourceServer resourceServer = resource.getResourceServer();
                List<PermissionTicket> permissions = ticketStore.findByResource(resourceServer, resource);

                for (PermissionTicket permission : permissions) {
                    ticketStore.delete(permission.getId());
                }

                PolicyStore policyStore = storeFactory.getPolicyStore();
                List<Policy> policies = policyStore.findByResource(resourceServer, resource);

                for (Policy policyModel : policies) {
                    if (policyModel.getResources().size() == 1 && !AdminPermissionsSchema.SCHEMA.isAdminPermissionClient(realm, resourceServer.getId())) {
                        policyStore.delete(policyModel.getId());
                    } else {
                        policyModel.removeResource(resource);
                    }
                }

                delegate.delete(id);
            }

            @Override
            public Resource findById(ResourceServer resourceServer, String id) {
                return delegate.findById(resourceServer, id);
            }

            @Override
            public List<Resource> findByOwner(ResourceServer resourceServer, String ownerId) {
                return delegate.findByOwner(resourceServer, ownerId);
            }

            @Override
            public void findByOwner(ResourceServer resourceServer, String ownerId, Consumer<Resource> consumer) {
                delegate.findByOwner(resourceServer, ownerId, consumer);
            }

            @Override
            public List<Resource> findByResourceServer(ResourceServer resourceServer) {
                return delegate.findByResourceServer(resourceServer);
            }

            @Override
            public List<Resource> find(ResourceServer resourceServer, Map<Resource.FilterOption, String[]> attributes, Integer firstResult, Integer maxResults) {
                return delegate.find(resourceServer, attributes, firstResult, maxResults);
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
