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
package org.keycloak.authorization.admin;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.authorization.PolicyEvaluationRequest;
import org.keycloak.authorization.admin.representation.PolicyEvaluationResponseBuilder;
import org.keycloak.authorization.attribute.Attributes;
import org.keycloak.authorization.common.KeycloakEvaluationContext;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.DecisionResultCollector;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.authorization.policy.evaluation.Result;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.authorization.util.Permissions;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.admin.RealmAuth;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyEvaluationService {

    private final AuthorizationProvider authorization;
    private final RealmAuth auth;
    @Context
    private HttpRequest httpRequest;

    private final ResourceServer resourceServer;

    PolicyEvaluationService(ResourceServer resourceServer, AuthorizationProvider authorization, RealmAuth auth) {
        this.resourceServer = resourceServer;
        this.authorization = authorization;
        this.auth = auth;
    }

    static class Decision extends DecisionResultCollector {
        Throwable error;
        List<Result> results;

        @Override
        protected void onComplete(List<Result> results) {
            this.results = results;
        }

        @Override
        public void onError(Throwable cause) {
            this.error = cause;

        }
    }

    public static <T> List<T> asList(T... a) {
        List<T> list = new LinkedList<T>();
        for (T t : a) list.add(t);
        return list;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response evaluate(PolicyEvaluationRequest evaluationRequest) throws Throwable {
        this.auth.requireView();
        CloseableKeycloakIdentity identity = createIdentity(evaluationRequest);
        try {
            EvaluationContext evaluationContext = createEvaluationContext(evaluationRequest, identity);
            Decision decisionCollector = new Decision();
            authorization.evaluators().from(createPermissions(evaluationRequest, evaluationContext, authorization), evaluationContext).evaluate(decisionCollector);
            if (decisionCollector.error != null) {
                throw decisionCollector.error;
            }
            return Response.ok(PolicyEvaluationResponseBuilder.build(decisionCollector.results, resourceServer, authorization, identity)).build();
        } finally {
            identity.close();
        }
    }

    private EvaluationContext createEvaluationContext(PolicyEvaluationRequest representation, KeycloakIdentity identity) {
        return new KeycloakEvaluationContext(identity, this.authorization.getKeycloakSession()) {
            @Override
            public Attributes getAttributes() {
                Map<String, Collection<String>> attributes = new HashMap<>(super.getAttributes().toMap());
                Map<String, String> givenAttributes = representation.getContext().get("attributes");

                if (givenAttributes != null) {
                    givenAttributes.forEach((key, entryValue) -> {
                        if (entryValue != null) {
                            List<String> values = new ArrayList();

                            for (String value : entryValue.split(",")) {
                                values.add(value);
                            }

                            attributes.put(key, values);
                        }
                    });
                }

                return Attributes.from(attributes);
            }
        };
    }

    private List<ResourcePermission> createPermissions(PolicyEvaluationRequest representation, EvaluationContext evaluationContext, AuthorizationProvider authorization) {
        List<ResourceRepresentation> resources = representation.getResources();
        return resources.stream().flatMap((Function<ResourceRepresentation, Stream<ResourcePermission>>) resource -> {
            StoreFactory storeFactory = authorization.getStoreFactory();
            if (resource == null) {
                resource = new ResourceRepresentation();
            }

            Set<ScopeRepresentation> givenScopes = resource.getScopes();

            if (givenScopes == null) {
                givenScopes = new HashSet();
            }

            Set<String> scopeNames = givenScopes.stream().map(ScopeRepresentation::getName).collect(Collectors.toSet());

            if (resource.getId() != null) {
                Resource resourceModel = storeFactory.getResourceStore().findById(resource.getId(), resourceServer.getId());
                return Permissions.createResourcePermissions(resourceModel, scopeNames, authorization).stream();
            } else if (resource.getType() != null) {
                return storeFactory.getResourceStore().findByType(resource.getType(), resourceServer.getId()).stream().flatMap(resource1 -> Permissions.createResourcePermissions(resource1, scopeNames, authorization).stream());
            } else {
                ScopeStore scopeStore = storeFactory.getScopeStore();
                List<Scope> scopes = scopeNames.stream().map(scopeName -> scopeStore.findByName(scopeName, this.resourceServer.getId())).collect(Collectors.toList());
                List<ResourcePermission> collect = new ArrayList<ResourcePermission>();

                if (!scopes.isEmpty()) {
                    collect.addAll(scopes.stream().map(scope -> new ResourcePermission(null, asList(scope), resourceServer)).collect(Collectors.toList()));
                } else {
                    collect.addAll(Permissions.all(resourceServer, evaluationContext.getIdentity(), authorization));
                }

                return collect.stream();
            }
        }).collect(Collectors.toList());
    }

    private static class CloseableKeycloakIdentity extends KeycloakIdentity {
        private UserSessionModel userSession;
        private ClientSessionModel clientSession;

        public CloseableKeycloakIdentity(AccessToken accessToken, KeycloakSession keycloakSession, UserSessionModel userSession, ClientSessionModel clientSession) {
            super(accessToken, keycloakSession);
            this.userSession = userSession;
            this.clientSession = clientSession;
        }

        public void close() {
            if (clientSession != null) {
                keycloakSession.sessions().removeClientSession(realm, clientSession);
            }

            if (userSession != null) {
                keycloakSession.sessions().removeUserSession(realm, userSession);
            }

        }
    }

    private CloseableKeycloakIdentity createIdentity(PolicyEvaluationRequest representation) {
        KeycloakSession keycloakSession = this.authorization.getKeycloakSession();
        RealmModel realm = keycloakSession.getContext().getRealm();
        AccessToken accessToken = null;


        String subject = representation.getUserId();

        ClientSessionModel clientSession = null;
        UserSessionModel userSession = null;
        if (subject != null) {
            UserModel userModel = keycloakSession.users().getUserById(subject, realm);

            if (userModel != null) {
                String clientId = representation.getClientId();

                if (clientId == null) {
                    clientId = resourceServer.getClientId();
                }

                if (clientId != null) {
                    ClientModel clientModel = realm.getClientById(clientId);
                    clientSession = keycloakSession.sessions().createClientSession(realm, clientModel);
                    clientSession.setAuthMethod(OIDCLoginProtocol.LOGIN_PROTOCOL);
                    userSession = keycloakSession.sessions().createUserSession(realm, userModel, userModel.getUsername(), "127.0.0.1", "passwd", false, null, null);

                    new TokenManager().attachClientSession(userSession, clientSession);

                    Set<RoleModel> requestedRoles = new HashSet<>();
                    for (String roleId : clientSession.getRoles()) {
                        RoleModel role = realm.getRoleById(roleId);
                        if (role != null) {
                            requestedRoles.add(role);
                        }
                    }
                    accessToken = new TokenManager().createClientAccessToken(keycloakSession, requestedRoles, realm, clientModel, userModel, userSession, clientSession);
                }
            }
        }

        if (accessToken == null) {
            accessToken = new AccessToken();

            accessToken.subject(representation.getUserId());
            accessToken.issuedFor(representation.getClientId());
            accessToken.audience(representation.getClientId());
            accessToken.issuer(Urls.realmIssuer(keycloakSession.getContext().getUri().getBaseUri(), realm.getName()));
            accessToken.setRealmAccess(new AccessToken.Access());

        }

        AccessToken.Access realmAccess = accessToken.getRealmAccess();
        Map<String, Object> claims = accessToken.getOtherClaims();
        Map<String, String> givenAttributes = representation.getContext().get("attributes");

        if (givenAttributes != null) {
            givenAttributes.forEach((key, value) -> claims.put(key, asList(value)));
        }


        if (representation.getRoleIds() != null) {
            representation.getRoleIds().forEach(roleName -> realmAccess.addRole(roleName));
        }

        return new CloseableKeycloakIdentity(accessToken, keycloakSession, userSession, clientSession);
    }
}