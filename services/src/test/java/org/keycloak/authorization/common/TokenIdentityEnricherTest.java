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

import org.keycloak.representations.AccessToken;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Boundary-condition tests for {@link TokenIdentityEnricher}.
 *
 * <p>Functional coverage of the enrichment behavior (realm vs. client roles,
 * cross-client role projection) is provided by the integration test
 * {@code org.keycloak.testsuite.authz.KeycloakIdentityCrossClientRoleTest},
 * which exercises the helper end-to-end against a running Keycloak session.
 */
class TokenIdentityEnricherTest {

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
}
