/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.common;

import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;

/**
 * Utilities for projecting a user's full role assignments onto an
 * {@link AccessToken} so that authorization evaluation has visibility into
 * roles that are otherwise filtered out by OIDC scope configuration.
 *
 * <p>A {@link KeycloakIdentity} constructed directly from a bearer access
 * token derives its role attributes from the token's
 * {@code realm_access} and {@code resource_access} claims. When a policy
 * references a role defined in a client other than the resource server, and
 * that client is not part of the requesting client's scope, the role will
 * be absent from the token and the policy will evaluate to DENY even when
 * the user has been granted the role.
 *
 * <p>This helper centralizes the enrichment logic the Keycloak admin
 * console already uses internally when evaluating policies on behalf of a
 * user. Extensions performing programmatic permission evaluation can use it
 * to obtain a {@code KeycloakIdentity} consistent with the admin console's
 * behavior:
 *
 * <pre>{@code
 * AccessToken token = Tokens.getAccessToken(session);
 * UserModel user = session.users().getUserById(realm, token.getSubject());
 * TokenIdentityEnricher.addAllUserRoles(token, user);
 * Identity identity = new KeycloakIdentity(token, session, realm);
 * }</pre>
 */
public final class TokenIdentityEnricher {

    private TokenIdentityEnricher() {
    }

    /**
     * Adds every role mapping of {@code user} to {@code token}. Realm roles
     * are added to {@code realm_access}; client roles are added to
     * {@code resource_access} under their owning client. Existing roles on
     * the token are preserved.
     *
     * @param token the access token to enrich; must not be {@code null}
     * @param user  the user whose role mappings will be projected onto the
     *              token; must not be {@code null}
     * @throws IllegalArgumentException if {@code token} or {@code user} is
     *                                  {@code null}
     */
    public static void addAllUserRoles(AccessToken token, UserModel user) {
        if (token == null) {
            throw new IllegalArgumentException("token must not be null");
        }
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }

        user.getRoleMappingsStream().forEach(roleModel -> {
            if (roleModel.isClientRole()) {
                ClientModel client = (ClientModel) roleModel.getContainer();
                token.addAccess(client.getClientId()).addRole(roleModel.getName());
            } else {
                AccessToken.Access realmAccess = token.getRealmAccess();
                if (realmAccess == null) {
                    realmAccess = new AccessToken.Access();
                    token.setRealmAccess(realmAccess);
                }
                realmAccess.addRole(roleModel.getName());
            }
        });
    }
}
