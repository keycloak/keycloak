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
package org.keycloak.protocol.oidc.mappers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class AbstractUserRoleMappingMapperTest {

    @Test
    public void testRolesFromMultipleMappersToSameNestedClaimAreNotDuplicated() {
        IDToken token = new IDToken();
        ProtocolMapperModel mapperA = UserRealmRoleMappingMapper.create(null, "scopeA roles", "realm_access.roles", true, true, true, true);
        ProtocolMapperModel mapperB = UserRealmRoleMappingMapper.create(null, "scopeB roles", "realm_access.roles", true, true, true, true);

        AbstractUserRoleMappingMapper.setClaim(token, mapperA, Set.of("roleA", "roleB"), null, null);
        AbstractUserRoleMappingMapper.setClaim(token, mapperB, Set.of("roleA", "roleB"), null, null);

        Map<?, ?> realmAccess = (Map<?, ?>) token.getOtherClaims().get("realm_access");
        assertThat((List<?>) realmAccess.get("roles"), Matchers.containsInAnyOrder("roleA", "roleB"));
    }

    @Test
    public void testRolesFromMultipleMappersToSameClaimAreNotDuplicated() {
        AccessToken token = new AccessToken();
        ProtocolMapperModel mapperA = UserRealmRoleMappingMapper.create(null, "scopeA roles", "roles", true, true, true, true);
        ProtocolMapperModel mapperB = UserRealmRoleMappingMapper.create(null, "scopeB roles", "roles", true, true, true, true);

        AbstractUserRoleMappingMapper.setClaim(token, mapperA, Set.of("roleA"), null, null);
        AbstractUserRoleMappingMapper.setClaim(token, mapperB, Set.of("roleA", "roleB"), null, null);

        assertThat((List<?>) token.getOtherClaims().get("roles"), Matchers.containsInAnyOrder("roleA", "roleB"));
    }
}
