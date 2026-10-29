/*
 * Copyright 2026 Andrzej Świętek
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
package org.keycloak.broker.provider.mappersync;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.keycloak.models.utils.KeycloakModelUtils;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class RoleReferenceTest {

    private static final Predicate<String> CLIENT_IDS = Set.of("app", "app.child")::contains;

    @Test
    public void longestExistingClientIdNamesTheClient() {
        assertThat(RoleReference.resolve("app.child.viewer", CLIENT_IDS),
                equalTo(Optional.of(new RoleReference(Optional.of("app.child"), "viewer"))));
        assertThat(RoleReference.resolve("app.other.viewer", CLIENT_IDS),
                equalTo(Optional.of(new RoleReference(Optional.of("app"), "other.viewer"))));
    }

    @Test
    public void referenceWithoutClientIdNamesARealmRole() {
        assertThat(RoleReference.resolve("tenant.admin", CLIENT_IDS),
                equalTo(Optional.of(new RoleReference(Optional.empty(), "tenant.admin"))));
        assertThat(RoleReference.resolve("tenant-admin", CLIENT_IDS),
                equalTo(Optional.of(new RoleReference(Optional.empty(), "tenant-admin"))));
    }

    @Test
    public void referenceWithTooManyClientIdCandidatesNamesNoRole() {
        String reference = "a" + ".a".repeat(KeycloakModelUtils.MAX_CLIENT_LOOKUPS_DURING_ROLE_RESOLVE);

        assertThat(RoleReference.resolve(reference, CLIENT_IDS), equalTo(Optional.empty()));
        assertThat(RoleReference.resolve(reference.substring(2), CLIENT_IDS),
                equalTo(Optional.of(new RoleReference(Optional.empty(), reference.substring(2)))));
    }
}
