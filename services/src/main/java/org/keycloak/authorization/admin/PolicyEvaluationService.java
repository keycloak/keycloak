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

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.admin.representation.PolicyEvaluationRequest;
import org.keycloak.authorization.admin.representation.PolicyEvaluationResponse;
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
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.authorization.util.Permissions;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.Urls;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyEvaluationService {

    private final AuthorizationProvider authorization;
    @Context
    private HttpRequest httpRequest;

    private final ResourceServer resourceServer;

    PolicyEvaluationService(ResourceServer resourceServer, AuthorizationProvider authorization) {
        this.resourceServer = resourceServer;
        this.authorization = authorization;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public void evaluate(PolicyEvaluationRequest evaluationRequest, @Suspended AsyncResponse asyncResponse) {
        EvaluationContext evaluationContext = createEvaluationContext(evaluationRequest);
        authorization.evaluators().from(createPermissions(evaluationRequest, evaluationContext, authorization), evaluationContext).evaluate(createDecisionCollector(evaluationRequest, authorization, asyncResponse));
    }

    private DecisionResultCollector createDecisionCollector(PolicyEvaluationRequest evaluationRequest, AuthorizationProvider authorization, AsyncResponse asyncResponse) {
        return new DecisionResultCollector() {
            @Override
            protected void onComplete(List<Result> results) {
                try {
                    asyncResponse.resume(Response.ok(PolicyEvaluationResponse.build(evaluationRequest,  results, resourceServer,  authorization)).build());
                } catch (Throwable cause) {
                    asyncResponse.resume(cause);
                }
            }

            @Override
            public void onError(Throwable cause) {
                asyncResponse.resume(cause);
            }
        };
    }

    private EvaluationContext createEvaluationContext(PolicyEvaluationRequest representation) {
        return new KeycloakEvaluationContext(createIdentity(representation), this.authorization.getKeycloakSession()) {
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
        if (representation.isEntitlements()) {
            return Permissions.all(this.resourceServer, evaluationContext.getIdentity(), authorization);
        }

        return representation.getResources().stream().flatMap((Function<PolicyEvaluationRequest.Resource, Stream<ResourcePermission>>) resource -> {
            Set<String> givenScopes = resource.getScopes();

            if (givenScopes == null) {
                givenScopes = new HashSet();
            }

            StoreFactory storeFactory = authorization.getStoreFactory();

            List<Scope> scopes = givenScopes.stream().map(scopeName -> storeFactory.getScopeStore().findByName(scopeName, this.resourceServer.getId())).collect(Collectors.toList());

            if (resource.getId() != null) {
                Resource resourceModel = storeFactory.getResourceStore().findById(resource.getId());
                return Stream.of(new ResourcePermission(resourceModel, scopes, resourceServer));
            } else if (resource.getType() != null) {
                return storeFactory.getResourceStore().findByType(resource.getType()).stream().map(resource1 -> new ResourcePermission(resource1, scopes, resourceServer));
            } else {
                return scopes.stream().map(scope -> new ResourcePermission(null, asList(scope), resourceServer));
            }
        }).collect(Collectors.toList());
    }

    private KeycloakIdentity createIdentity(PolicyEvaluationRequest representation) {
        RealmModel realm = this.authorization.getKeycloakSession().getContext().getRealm();
        AccessToken accessToken = new AccessToken();

        accessToken.subject(representation.getUserId());
        accessToken.issuedFor(representation.getClientId());
        accessToken.audience(representation.getClientId());
        accessToken.issuer(Urls.realmIssuer(this.authorization.getKeycloakSession().getContext().getUri().getBaseUri(), realm.getName()));
        accessToken.setRealmAccess(new AccessToken.Access());

        Map<String, Object> claims = accessToken.getOtherClaims();
        Map<String, String> givenAttributes = representation.getContext().get("attributes");

        if (givenAttributes != null) {
            givenAttributes.forEach((key, value) -> claims.put(key, asList(value)));
        }

        String subject = accessToken.getSubject();

        if (subject != null) {
            UserModel userModel = this.authorization.getKeycloakSession().users().getUserById(subject, realm);

            if (userModel != null) {
                Set<RoleModel> roleMappings = userModel.getRoleMappings();

                roleMappings.stream().map(RoleModel::getName).forEach(roleName -> accessToken.getRealmAccess().addRole(roleName));

                String clientId = representation.getClientId();

                if (clientId != null) {
                    ClientModel clientModel = realm.getClientById(clientId);

                    accessToken.addAccess(clientModel.getClientId());

                    userModel.getClientRoleMappings(clientModel).stream().map(RoleModel::getName).forEach(roleName -> accessToken.getResourceAccess(clientModel.getClientId()).addRole(roleName));

                    //TODO: would be awesome if we could transform the access token using the configured protocol mappers. Tried, but without a clientSession and userSession is tuff.
                }
            }
        }

        if (representation.getRoleIds() != null) {
            representation.getRoleIds().forEach(roleName -> accessToken.getRealmAccess().addRole(roleName));
        }

        return new KeycloakIdentity(accessToken, this.authorization.getKeycloakSession());
    }
}