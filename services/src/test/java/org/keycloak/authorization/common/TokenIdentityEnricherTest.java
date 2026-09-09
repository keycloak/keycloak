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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.representations.AccessToken;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TokenIdentityEnricher}: the null-argument contract and
 * the realm-role projection, including the case where the token carries no
 * {@code realm_access} claim yet and the enricher has to create it.
 *
 * <p>The client-role branch resolves the role's owning {@link RoleContainerModel}
 * as a client, which is impractical to fake at this level; it is covered
 * end-to-end, together with the cross-client projection this helper exists for,
 * by the integration test
 * {@code org.keycloak.tests.authz.services.KeycloakIdentityCrossClientRoleTest}.
 */
class TokenIdentityEnricherTest {

    private static final String REALM_ROLE = "realm-role";

    @Test
    void addAllUserRoles_rejectsNullToken() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TokenIdentityEnricher.addAllUserRoles(null, null));
        assertEquals("token must not be null", ex.getMessage());
    }

    @Test
    void addAllUserRoles_rejectsNullUser() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TokenIdentityEnricher.addAllUserRoles(new AccessToken(), null));
        assertEquals("user must not be null", ex.getMessage());
    }

    @Test
    void addAllUserRoles_createsRealmAccess_whenTokenHasNone() {
        AccessToken token = new AccessToken();

        TokenIdentityEnricher.addAllUserRoles(token, userWithRoles(new FakeRealmRole(REALM_ROLE)));

        AccessToken.Access realmAccess = token.getRealmAccess();
        assertNotNull(realmAccess, "realm_access should have been created for the projected realm role");
        assertEquals(Set.of(REALM_ROLE), realmAccess.getRoles());
    }

    @Test
    void addAllUserRoles_preservesExistingRealmRoles() {
        AccessToken token = new AccessToken();
        token.setRealmAccess(new AccessToken.Access().addRole("pre-existing"));

        TokenIdentityEnricher.addAllUserRoles(token, userWithRoles(new FakeRealmRole(REALM_ROLE)));

        AccessToken.Access realmAccess = token.getRealmAccess();
        assertTrue(realmAccess.isUserInRole("pre-existing"), "existing realm roles must be preserved");
        assertTrue(realmAccess.isUserInRole(REALM_ROLE), "projected realm role must be added");
    }

    private static UserModel userWithRoles(RoleModel... roles) {
        return new UserModelDelegate(null) {
            @Override
            public Stream<RoleModel> getRoleMappingsStream() {
                return Stream.of(roles);
            }
        };
    }

    /**
     * Minimal realm {@link RoleModel} double, in the spirit of the {@code FakeRole}
     * used by {@code RoleProviderCompositeDefaultTest}: only the surface the
     * enricher touches is implemented.
     */
    private static final class FakeRealmRole implements RoleModel {

        private final String name;

        FakeRealmRole(String name) {
            this.name = name;
        }

        @Override public String getName() { return name; }
        @Override public boolean isClientRole() { return false; }

        // --- unused RoleModel surface --------------------------------------------------------

        @Override public String getDescription() { throw new UnsupportedOperationException(); }
        @Override public void setDescription(String description) { throw new UnsupportedOperationException(); }
        @Override public String getId() { throw new UnsupportedOperationException(); }
        @Override public void setName(String name) { throw new UnsupportedOperationException(); }
        @Override public boolean isComposite() { throw new UnsupportedOperationException(); }
        @Override public void addCompositeRole(RoleModel role) { throw new UnsupportedOperationException(); }
        @Override public void removeCompositeRole(RoleModel role) { throw new UnsupportedOperationException(); }
        @Override public Stream<RoleModel> getCompositesStream(String search, Integer first, Integer max) { throw new UnsupportedOperationException(); }
        @Override public String getContainerId() { throw new UnsupportedOperationException(); }
        @Override public RoleContainerModel getContainer() { throw new UnsupportedOperationException(); }
        @Override public boolean hasRole(RoleModel role) { throw new UnsupportedOperationException(); }
        @Override public void setSingleAttribute(String name, String value) { throw new UnsupportedOperationException(); }
        @Override public void setAttribute(String name, List<String> values) { throw new UnsupportedOperationException(); }
        @Override public void removeAttribute(String name) { throw new UnsupportedOperationException(); }
        @Override public Stream<String> getAttributeStream(String name) { throw new UnsupportedOperationException(); }
        @Override public Map<String, List<String>> getAttributes() { throw new UnsupportedOperationException(); }
    }
}
