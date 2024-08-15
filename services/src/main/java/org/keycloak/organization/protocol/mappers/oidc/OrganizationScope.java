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

import static org.keycloak.organization.utils.Organizations.getProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.keycloak.common.util.TriFunction;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeDecorator;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.utils.StringUtil;

/**
 * An enum with utility methods to process the {@link OIDCLoginProtocolFactory#ORGANIZATION} scope.
 */
public enum OrganizationScope {

    /**
     * Maps to any organization a user is a member
     */
    ALL("*"::equals,
            (organizations) -> true,
            (user, scopes, session) -> {
                if (user == null) {
                    return Stream.empty();
                }
                return getProvider(session).getByMember(user).filter(OrganizationModel::isEnabled);
            }),

    /**
     * Maps to a specific organization the user is a member.
     */
    SINGLE(StringUtil::isNotBlank,
            (organizations) -> organizations.findAny().isPresent(),
            (user, scopes, session) -> {
                OrganizationModel organization = parseScopeParameter(scopes)
                        .map(OrganizationScope::parseScopeValue)
                        .map(alias -> getProvider(session).getByAlias(alias))
                        .filter(Objects::nonNull)
                        .filter(OrganizationModel::isEnabled)
                        .findAny()
                        .orElse(null);

                if (organization == null) {
                    return Stream.empty();
                }

                if (user == null || organization.isMember(user)) {
                    return Stream.of(organization);
                }

                return Stream.empty();
            }),

    /**
     * Maps to a single organization if the user is a member of a single organization.
     */
    ANY(""::equals,
            (organizations) -> true,
            (user, scopes, session) -> {
                List<OrganizationModel> organizations = getProvider(session).getByMember(user).toList();

                if (organizations.size() == 1) {
                    return organizations.stream();
                }

                return Stream.empty();
            });

    private static final Pattern SCOPE_PATTERN = Pattern.compile(OIDCLoginProtocolFactory.ORGANIZATION + ":*".replace("*", "(.*)"));
    private final Predicate<String> valueMatcher;
    private final Predicate<Stream<OrganizationModel>> valueValidator;
    private final TriFunction<UserModel, String, KeycloakSession, Stream<OrganizationModel>> orgResolver;

    OrganizationScope(Predicate<String> valueMatcher, Predicate<Stream<OrganizationModel>> valueValidator, TriFunction<UserModel, String, KeycloakSession, Stream<OrganizationModel>> orgResolver) {
        this.valueMatcher = valueMatcher;
        this.valueValidator = valueValidator;
        this.orgResolver = orgResolver;
    }

    /**
     * Returns the organizations mapped from the {@code scope} based on the given {@code user}.
     *
     * @param user the user. Can be {@code null} depending on how the scope resolves its value.
     * @param scope the string referencing the scope
     * @param session the session
     * @return the organizations mapped to the given {@code user}. Or an empty stream if no organizations were mapped from the {@code scope} parameter.
     */
    public Stream<OrganizationModel> resolveOrganizations(UserModel user, String scope, KeycloakSession session) {
        if (scope == null) {
            return Stream.empty();
        }
        return orgResolver.apply(user, scope, session);
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
        KeycloakContext context = session.getContext();
        ClientModel client = context.getClient();
        ClientScopeModel orgScope = getOrganizationClientScope(client, session);

        if (orgScope == null) {
            return null;
        }

        OrganizationScope scope = OrganizationScope.valueOfScope(name);

        if (scope == null) {
            return null;
        }

        Stream<OrganizationModel> organizations = scope.resolveOrganizations(user, name, session);

        if (valueValidator.test(organizations)) {
            return new ClientScopeDecorator(orgScope, name);
        }

        return null;
    }

    /**
     * Returns a {@link OrganizationScope} instance based on the given {@code rawScope}.
     *
     * @param rawScope the string referencing the scope
     * @return the organization scope that maps the given {@code rawScope}
     */
    public static OrganizationScope valueOfScope(String rawScope) {
        if (rawScope == null) {
            return null;
        }
        return parseScopeParameter(rawScope)
                .map(s -> {
                    for (OrganizationScope scope : values()) {
                        if (scope.valueMatcher.test(parseScopeValue(s))) {
                            return scope;
                        }
                    }
                    return null;
                }).filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    private static String parseScopeValue(String scope) {
        if (!hasOrganizationScope(scope)) {
            return null;
        }

        if (scope.equals(OIDCLoginProtocolFactory.ORGANIZATION)) {
            return "";
        }

        Matcher matcher = SCOPE_PATTERN.matcher(scope);

        if (matcher.matches()) {
            return matcher.group(1);
        }

        return null;
    }

    private ClientScopeModel getOrganizationClientScope(ClientModel client, KeycloakSession session) {
        if (!Organizations.isEnabledAndOrganizationsPresent(session)) {
            return null;
        }

        Map<String, ClientScopeModel> scopes = new HashMap<>(client.getClientScopes(true));
        scopes.putAll(client.getClientScopes(false));

        return scopes.get(OIDCLoginProtocolFactory.ORGANIZATION);
    }

    private static boolean hasOrganizationScope(String scope) {
        return scope != null && scope.contains(OIDCLoginProtocolFactory.ORGANIZATION);
    }

    private static Stream<String> parseScopeParameter(String rawScope) {
        return TokenManager.parseScopeParameter(rawScope)
                .filter(OrganizationScope::hasOrganizationScope);
    }
}
