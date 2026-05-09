/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.authzen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.attribute.Attributes;
import org.keycloak.authorization.common.ClientModelIdentity;
import org.keycloak.authorization.common.DefaultEvaluationContext;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.identity.UserModelIdentity;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.util.JsonSerialization;

public class AuthZenResource {

    private static final AuthZen.EvaluationResponse DECISION_FALSE = new AuthZen.EvaluationResponse(false);
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");

    private static final String NAMESPACE_ID = "id:";
    private static final String NAMESPACE_USERNAME = "username:";
    private static final String NAMESPACE_EMAIL = "email:";

    private final KeycloakSession session;

    public AuthZenResource(KeycloakSession session) {
        this.session = session;
    }

    @POST
    @Path(AuthZen.EVALUATION_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response evaluate(AuthZen.EvaluationRequest request) {
        AccessToken token = Tokens.getAccessToken(session);
        if (token == null) {
            throw new NotAuthorizedException("Bearer");
        }
        return Response.ok(evaluateSingle(request, token)).build();
    }

    @POST
    @Path(AuthZen.EVALUATIONS_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response evaluations(AuthZen.EvaluationsRequest request) {
        AccessToken token = Tokens.getAccessToken(session);
        if (token == null) {
            throw new NotAuthorizedException("Bearer");
        }

        if (request.evaluations() == null || request.evaluations().isEmpty()) {
            // AuthZen 1.0 Section 7.1
            // "If an evaluations array is NOT present or is empty, the Access Evaluations Request behaves in a
            // backwards-compatible manner with the (single) Access Evaluation API Request (Section 6.1)."
            AuthZen.EvaluationRequest single = new AuthZen.EvaluationRequest(
                  request.subject(), request.resource(), request.action(), request.context());
            return Response.ok(evaluateSingle(single, token)).build();
        }

        AuthZen.EvaluationsSemantic semantic = AuthZen.EvaluationsSemantic.EXECUTE_ALL;
        if (request.options() != null && request.options().evaluationsSemantic() != null) {
            semantic = request.options().evaluationsSemantic();
        }

        List<AuthZen.EvaluationResponse> results = new ArrayList<>(request.evaluations().size());
        for (AuthZen.EvaluationItem item : request.evaluations()) {
            AuthZen.EvaluationRequest merged = mergeDefaults(request, item);
            AuthZen.EvaluationResponse itemResponse = evaluateSingle(merged, token);

            if (semantic == AuthZen.EvaluationsSemantic.DENY_ON_FIRST_DENY && !itemResponse.decision()) {
                results.add(new AuthZen.EvaluationResponse(false, Map.of("reason", "deny_on_first_deny")));
                break;
            }

            results.add(itemResponse);

            if (semantic == AuthZen.EvaluationsSemantic.PERMIT_ON_FIRST_PERMIT && itemResponse.decision()) {
                break;
            }
        }
        return Response.ok(new AuthZen.EvaluationsResponse(results)).build();
    }

    private static AuthZen.EvaluationRequest mergeDefaults(AuthZen.EvaluationsRequest defaults, AuthZen.EvaluationItem item) {
        AuthZen.Subject subject = item.subject() != null ? item.subject() : defaults.subject();
        AuthZen.Resource resource = item.resource() != null ? item.resource() : defaults.resource();
        AuthZen.Action action = item.action() != null ? item.action() : defaults.action();
        Map<String, Object> context = item.context() != null ? item.context() : defaults.context();
        return new AuthZen.EvaluationRequest(subject, resource, action, context);
    }

    private AuthZen.EvaluationResponse evaluateSingle(AuthZen.EvaluationRequest request, AccessToken token) {
        if (request.subject() == null || request.resource() == null || request.action() == null) {
            return new AuthZen.EvaluationResponse(false);
        }

        RealmModel realm = session.getContext().getRealm();
        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        StoreFactory storeFactory = authorization.getStoreFactory();

        ClientModel client;
        if (request.subject().type() == AuthZen.SubjectType.CLIENT) {
            if (!request.subject().id().equals(token.getIssuedFor())) {
                return DECISION_FALSE;
            }
            client = realm.getClientByClientId(request.subject().id());
        } else {
            client = realm.getClientByClientId(token.getIssuedFor());
        }

        if (client == null || !client.isEnabled()) {
            return DECISION_FALSE;
        }

        ResourceServer resourceServer = storeFactory.getResourceServerStore().findByClient(client);
        if (resourceServer == null) {
            return DECISION_FALSE;
        }

        Identity identity = resolveSubjectIdentity(realm, request);
        if (identity == null) {
            return DECISION_FALSE;
        }

        ResourceStore resourceStore = storeFactory.getResourceStore();
        Resource resource = resourceStore.findByName(resourceServer, request.resource().id());
        if (resource == null) {
            return DECISION_FALSE;
        }

        String keycloakType = resource.getType() != null ? resource.getType() : "";
        if (!keycloakType.equals(request.resource().type())) {
            return DECISION_FALSE;
        }

        ScopeStore scopeStore = storeFactory.getScopeStore();
        Scope scope = scopeStore.findByName(resourceServer, request.action().name());
        if (scope == null) {
            return DECISION_FALSE;
        }

        Map<String, List<String>> claims = null;
        if (request.resource().properties() != null) {
            claims = new HashMap<>();
            convertContext(request.resource().properties(), claims);
        }
        if (request.context() != null) {
            if (claims == null) {
                claims = new HashMap<>();
            }
            convertContext(request.context(), claims);
        }

        DefaultEvaluationContext context = new DefaultEvaluationContext(identity, claims, session);
        ResourcePermission permission = new ResourcePermission(resource, List.of(scope), resourceServer);

        Collection<Permission> granted = authorization.evaluators()
              .from(List.of(permission), context)
              .evaluate(resourceServer, null);

        return new AuthZen.EvaluationResponse(!granted.isEmpty());
    }

    private Identity resolveSubjectIdentity(RealmModel realm, AuthZen.EvaluationRequest request) {
        AuthZen.Subject subject = request.subject();
        Identity identity = switch (subject.type()) {
            case USER -> {
                UserModel user = resolveUserId(realm, subject.id());
                yield user != null ? new UserModelIdentity(realm, user) : null;
            }
            case CLIENT -> {
                ClientModel subjectClient = realm.getClientByClientId(subject.id());
                yield subjectClient != null ? new ClientModelIdentity(session, subjectClient) : null;
            }
        };
        if (request.subject().properties() != null && !request.subject().properties().isEmpty()) {
            identity = withSubjectProperties(identity, request.subject().properties());
        }
        return identity;
    }

    private UserModel resolveUserId(RealmModel realm, String subjectId) {
        UserProvider users = session.users();
        if (subjectId.startsWith(NAMESPACE_ID)) {
            String id = extractNamespaceValue(subjectId, NAMESPACE_ID);
            return users.getUserById(realm, id);
        } else if (subjectId.startsWith(NAMESPACE_USERNAME)) {
            String username = extractNamespaceValue(subjectId, NAMESPACE_USERNAME);
            return users.getUserByUsername(realm, username);
        } else if (subjectId.startsWith(NAMESPACE_EMAIL)) {
            if (realm.isDuplicateEmailsAllowed()) {
                throw new BadRequestException("email namespace cannot be used when duplicate emails are allowed");
            }
            String email = extractNamespaceValue(subjectId, NAMESPACE_EMAIL);
            return users.getUserByEmail(realm, email);
        } else if (UUID_PATTERN.matcher(subjectId).matches()) {
            return users.getUserById(realm, subjectId);
        } else {
            return users.getUserByUsername(realm, subjectId);
        }
    }

    private static String extractNamespaceValue(String subjectId, String namespace) {
        String value = subjectId.substring(namespace.length());
        if (value.isEmpty()) {
            throw new BadRequestException("subject id namespace '" + namespace + "' requires a non-empty value");
        }
        return value;
    }

    private static Identity withSubjectProperties(Identity delegate, Map<String, Object> properties) {
        Map<String, Collection<String>> extra = new HashMap<>();
        convertValues(properties, extra);

        return new Identity() {
            @Override
            public String getId() {
                return delegate.getId();
            }

            @Override
            public Attributes getAttributes() {
                Map<String, Collection<String>> merged = new HashMap<>(delegate.getAttributes().toMap());
                merged.putAll(extra);
                return Attributes.from(merged);
            }

            @Override
            public boolean hasRealmRole(String roleName) {
                return delegate.hasRealmRole(roleName);
            }

            @Override
            public boolean hasClientRole(String clientId, String roleName) {
                return delegate.hasClientRole(clientId, roleName);
            }

            @Override
            public boolean hasOneClientRole(String clientId, String... roleNames) {
                return delegate.hasOneClientRole(clientId, roleNames);
            }
        };
    }

    private static void convertValues(Map<String, Object> source, Map<String, ? extends Collection<String>> result) {
        @SuppressWarnings("unchecked")
        Map<String, Collection<String>> target = (Map<String, Collection<String>>) result;
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map || value instanceof List) {
                try {
                    target.put(entry.getKey(), List.of(JsonSerialization.writeValueAsString(value)));
                } catch (Exception e) {
                    target.put(entry.getKey(), List.of(String.valueOf(value)));
                }
            } else {
                target.put(entry.getKey(), List.of(String.valueOf(value)));
            }
        }
    }

    private static void convertContext(Map<String, Object> context, Map<String, List<String>> result) {
        convertValues(context, result);
    }
}
