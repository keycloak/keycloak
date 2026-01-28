/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.organization.protocol.mappers.oidc;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.keycloak.common.util.TriFunction;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeDecorator;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

import static org.keycloak.models.ClientScopeModel.VALUE_SEPARATOR;
import static org.keycloak.organization.utils.Organizations.getProvider;
import static org.keycloak.utils.StringUtil.isBlank;

/**
 * <p>An enum with utility methods to process the {@link OIDCLoginProtocolFactory#ORGANIZATION} scope.
 *
 * <p>The {@link OrganizationScope} behaves like a dynamic scopes so that access to organizations is granted depending
 * on how the client requests the {@link OIDCLoginProtocolFactory#ORGANIZATION} scope.
 */
public enum OrganizationScope {

    /**
     * Maps to any organization a user is a member. When this scope is requested by clients, all the organizations
     * the user is a member are granted.
     */
    ALL("*"::equals,
            (user, scopes, session) -> {
                if (user == null) {
                    return Stream.empty();
                }
                return getProvider(session).getByMember(user);
            },
            (organizations) -> true,
            (session, current, previous) -> valueOfScope(session, current) == null ? previous : current),

    /**
     * Maps to a specific organization the user is a member. When this scope is requested by clients, only the
     * organization specified in the scope is granted.
     */
    SINGLE(StringUtil::isNotBlank,
            (user, scopes, session) -> {
                OrganizationModel organization = parseScopeParameter(session, scopes)
                        .map((String scope) -> parseScopeValue(session, scope))
                        .map(alias -> getProvider(session).getByAlias(alias))
                        .filter(Objects::nonNull)
                        .findAny()
                        .orElse(null);

                if (organization == null) {
                    return Stream.empty();
                }

                if (user == null || organization.isMember(user)) {
                    return Stream.of(organization);
                }

                return Stream.empty();
            },
            (organizations) -> organizations.findAny().isPresent(),
            (session, current, previous) -> {
                if (current.equals(previous)) {
                    return current;
                }

                if (OrganizationScope.ALL.equals(valueOfScope(session, current))) {
                    return previous;
                }

                return null;
            }),

    /**
     * Maps to a single organization if the user is a member of a single organization. When this scope is requested by clients,
     * the user will be asked to select and organization if a member of multiple organizations or, in case the user is a
     * member of a single organization, grant access to that organization.
     */
    ANY(""::equals,
            (user, scopes, session) -> {
                if (user == null) {
                    return Stream.empty();
                }

                List<OrganizationModel> organizations = getProvider(session).getByMember(user).toList();

                if (organizations.size() == 1) {
                    return organizations.stream();
                }

                ClientSessionContext context = (ClientSessionContext) session.getAttribute(ClientSessionContext.class.getName());

                if (context == null) {
                    return Stream.empty();
                }

                AuthenticatedClientSessionModel clientSession = context.getClientSession();
                String orgId = clientSession.getNote(OrganizationModel.ORGANIZATION_ATTRIBUTE);

                if (orgId == null) {
                    return Stream.empty();
                }

                return organizations.stream().filter(o -> o.getId().equals(orgId));
            },
            (organizations) -> true,
            (session, current, previous) -> {
                if (current.equals(previous)) {
                    return current;
                }

                if (OrganizationScope.ALL.equals(valueOfScope(session, current))) {
                    return previous;
                }

                return null;
            });

    private static final String ORGANIZATION_SCOPES_SESSION_ATTRIBUTE = "kc.org.client.scope";
    private static final String UNSUPPORTED_ORGANIZATION_SCOPES_ATTRIBUTE = "kc.org.client.scope.unsupported";
    private static final Pattern SCOPE_PATTERN = Pattern.compile("(.*)" + VALUE_SEPARATOR + "(.*)");
    private static final String EMPTY_SCOPE = "";

    /**
     * <p>Resolves the value of the scope from its raw format. For instance, {@code organization:<value>} will resolve to {@code <value>}.
     *
     * <p>If no value is provided, like in {@code organization}, an empty string is returned instead.
     */
    private final Predicate<String> valueMatcher;

    /**
     * Resolves the organizations of the user based on the values of the scope.
     */
    private final TriFunction<UserModel, String, KeycloakSession, Stream<OrganizationModel>> valueResolver;

    /**
     * Validate the value of the scope based on how they map to existing organizations.
     */
    private final Predicate<Stream<OrganizationModel>> valueValidator;

    /**
     * Resolves the name of the scope when requesting a scope using a different format.
     */
    private final TriFunction<KeycloakSession, String, String, String> nameResolver;

    OrganizationScope(Predicate<String> valueMatcher, TriFunction<UserModel, String, KeycloakSession, Stream<OrganizationModel>> valueResolver, Predicate<Stream<OrganizationModel>> valueValidator, TriFunction<KeycloakSession, String, String, String> nameResolver) {
        this.valueMatcher = valueMatcher;
        this.valueResolver = valueResolver;
        this.valueValidator = valueValidator;
        this.nameResolver = nameResolver;
    }

    /**
     * Returns the organizations mapped from the {@code scope} based on the given {@code user}.
     *
     * @param user the user. Can be {@code null} depending on how the scope resolves its value.
     * @param scope the string referencing the scope
     * @param session the session
     * @return the organizations mapped from the {@code scope} parameter. Or an empty stream if no organizations were mapped from the parameter.
     */
    public Stream<OrganizationModel> resolveOrganizations(UserModel user, String scope, KeycloakSession session) {
        return valueResolver.apply(user, Optional.ofNullable(scope).orElse(EMPTY_SCOPE), session).filter(OrganizationModel::isEnabled);
    }

    /**
     * Returns a stream of {@link OrganizationScope} instances based on the scopes from the {@code AuthenticationSessionModel} associated
     * with the given {@code session} and where the given {@code user} is a member.
     *
     * @param user the user. Can be {@code null} depending on how the scope resolves its value.
     * @param session the session
     * @return the organizations mapped from the {@code scope} parameter. Or an empty stream if no organizations were mapped from the parameter.
     */
    public Stream<OrganizationModel> resolveOrganizations(UserModel user, KeycloakSession session) {
        return resolveOrganizations(user, getRequestedScopes(session), session);
    }

    /**
     * Returns a stream of {@link OrganizationScope} instances based on the scopes from the {@code AuthenticationSessionModel} associated
     * with the given {@code session}.
     *
     * @param session the session
     * @return the organizations mapped from the {@code scope} parameter. Or an empty stream if no organizations were mapped from the parameter.
     */
    public Stream<OrganizationModel> resolveOrganizations(KeycloakSession session) {
        return resolveOrganizations(null, session);
    }

    /**
     * Returns a {@link ClientScopeModel} with the given {@code name} for this scope.
     *
     * @param name the name of the scope
     * @param user the user
     * @param session the session
     * @return the {@link ClientScopeModel}
     */
    public ClientScopeModel toClientScope(String name, UserModel user, KeycloakSession session) {
        OrganizationScope scope = valueOfScope(session, name);

        if (scope == null) {
            return null;
        }

        Stream<OrganizationModel> organizations = scope.resolveOrganizations(user, name, session);

        if (valueValidator.test(organizations)) {
            return new ClientScopeDecorator(resolveClientScope(session, name), name);
        }

        return null;
    }

    /**
     * <p>Resolves the name of this scope based on the given set of {@code scopes} and the {@code previous} name.
     *
     * <p>The scope name can be mapped to another scope depending on its semantics. Otherwise, it will map to
     * the same name. This method is mainly useful to recognize if a scope previously granted is still valid
     * and can be mapped to the new scope being requested. For instance, when refreshing tokens.
     *
     * @param scopes the scopes to resolve the name from
     * @param previous the previous name of this scope
     * @return the name of the scope
     */
    public String resolveName(KeycloakSession session, Set<String> scopes, String previous) {
        for (String scope : scopes) {
            String resolved = nameResolver.apply(session, scope, previous);

            if (resolved == null) {
                continue;
            }

            return resolved;
        }

        return null;
    }

    /**
     * Returns a {@link OrganizationScope} instance based on the given {@code rawScope}.
     *
     * @param rawScope the string referencing the scope
     * @return the organization scope that maps the given {@code rawScope}
     */
    public static OrganizationScope valueOfScope(KeycloakSession session, String rawScope) {
        return parseScopeParameter(session, Optional.ofNullable(rawScope).orElse(EMPTY_SCOPE))
                .map(s -> {
                    for (OrganizationScope scope : values()) {
                        if (scope.valueMatcher.test(parseScopeValue(session, s))) {
                            return scope;
                        }
                    }
                    return null;
                }).filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    /**
     * Returns a {@link OrganizationScope} instance based on the scopes from the {@code AuthenticationSessionModel} associated
     * with the given {@code session}.
     *
     * @param session the session
     * @return the organization scope that maps the given {@code rawScope}
     */
    public static OrganizationScope valueOfScope(KeycloakSession session) {
        OrganizationScope value = session.getAttribute(OrganizationScope.class.getName(), OrganizationScope.class);

        if (value != null) {
            return value;
        }

        value = valueOfScope(session, getRequestedScopes(session));

        if (value != null) {
            session.setAttribute(OrganizationScope.class.getName(), value);
        }

        return value;
    }

    private static String getRequestedScopes(KeycloakSession session) {
        AuthenticationSessionModel authSession = session.getContext().getAuthenticationSession();

        if (authSession == null) {
            return EMPTY_SCOPE;
        }

        String requestedScopes = authSession.getClientNote(OIDCLoginProtocol.SCOPE_PARAM);

        return Optional.ofNullable(requestedScopes).orElse(EMPTY_SCOPE);
    }

    private static String parseScopeValue(KeycloakSession session, String scope) {
        ClientScopeModel clientScope = resolveClientScope(session, scope);

        if (clientScope != null) {
            if (scope.equals(clientScope.getName())) {
                return "";
            }
        }

        Matcher matcher = SCOPE_PATTERN.matcher(scope);

        if (matcher.matches()) {
            return matcher.group(2);
        }

        return null;
    }

    private static Stream<String> parseScopeParameter(KeycloakSession session, String rawScope) {
        return TokenManager.parseScopeParameter(rawScope)
                .filter(scope -> resolveClientScope(session, scope) != null);
    }

    private static ClientScopeModel resolveClientScope(KeycloakSession session, String scope) {
        if (isBlank(scope)) {
            return null;
        }

        ClientModel client = session.getContext().getClient();

        if (client == null) {
            return null;
        }

        if (session.getAttributeOrDefault(UNSUPPORTED_ORGANIZATION_SCOPES_ATTRIBUTE, Set.of()).contains(scope)) {
            // scope already processed and does not support mapping organizations
            return null;
        }

        Set<ClientScopeModel> organizationScopes = session.getAttributeOrDefault(ORGANIZATION_SCOPES_SESSION_ATTRIBUTE, Set.of());

        for (ClientScopeModel clientScope : organizationScopes) {
            if (scope.equals(clientScope.getName()) || scope.startsWith(clientScope.getName() + VALUE_SEPARATOR)) {
                // scope already processed and supports organizations
                return clientScope;
            }
        }

        Matcher matcher = SCOPE_PATTERN.matcher(scope);

        if (matcher.matches()) {
            scope = matcher.group(1);
        }

        ClientScopeModel clientScope = getClientScope(client, scope);

        if (clientScope != null) {
            Stream<String> mappers = clientScope.getProtocolMappersStream().map(ProtocolMapperModel::getProtocolMapper);

            if (mappers.noneMatch(OrganizationMembershipMapper.PROVIDER_ID::equals)) {
                Set<String> nonOrganizationScopes = session.getAttributeOrDefault(UNSUPPORTED_ORGANIZATION_SCOPES_ATTRIBUTE, Set.of());

                if (nonOrganizationScopes.isEmpty()) {
                    nonOrganizationScopes = new HashSet<>();
                }

                // scope does not support organizations, cache the scope in this session to avoid processing it again
                nonOrganizationScopes.add(scope);
                session.setAttribute(UNSUPPORTED_ORGANIZATION_SCOPES_ATTRIBUTE, nonOrganizationScopes);

                return null;
            }

            organizationScopes = session.getAttributeOrDefault(ORGANIZATION_SCOPES_SESSION_ATTRIBUTE, Set.of());

            if (organizationScopes.isEmpty()) {
                organizationScopes = new HashSet<>();
            }

            organizationScopes.add(clientScope);
            // scope supports organizations, cache the scope in this session to avoid processing it again
            session.setAttribute(ORGANIZATION_SCOPES_SESSION_ATTRIBUTE, organizationScopes);
        }

        return clientScope;
    }

    private static ClientScopeModel getClientScope(ClientModel client, String scope) {
        ClientScopeModel clientScope = client.getClientScopes(false).get(scope);

        if (clientScope == null) {
            clientScope = client.getClientScopes(true).get(scope);
        }

        return clientScope;
    }
}
