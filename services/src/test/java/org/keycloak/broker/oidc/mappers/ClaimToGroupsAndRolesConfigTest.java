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

package org.keycloak.broker.oidc.mappers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.keycloak.broker.oidc.mappers.ClaimToGroupsAndRolesConfig.Invalid;
import org.keycloak.broker.oidc.mappers.ClaimToGroupsAndRolesConfig.Parsed;
import org.keycloak.broker.oidc.mappers.ClaimToGroupsAndRolesConfig.Valid;
import org.keycloak.broker.oidc.mappers.ClaimToGroupsAndRolesConfig.Violation;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

public class ClaimToGroupsAndRolesConfigTest {

    private static final String TABLE = "[{\"key\":\"admin\",\"value\":\"tenant-admin \"},{\"key\":\" viewer\",\"value\":\"tenant-viewer\"}]";

    private static Map<String, String> config(String... keysAndValues) {
        Map<String, String> config = new HashMap<>(Map.of(
                "claim", "entitlements",
                "delimiter", ",",
                "group.field.index", "0",
                "group", "/tenants",
                "role.field.index", "1",
                "roles", TABLE));
        for (int i = 0; i < keysAndValues.length; i += 2) {
            config.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return config;
    }

    private static ClaimToGroupsAndRolesConfig valid(Map<String, String> raw) {
        Parsed parsed = ClaimToGroupsAndRolesConfig.parse(raw);
        assertThat(parsed, instanceOf(Valid.class));
        return ((Valid) parsed).config();
    }

    private static List<String> violations(Map<String, String> raw) {
        Parsed parsed = ClaimToGroupsAndRolesConfig.parse(raw);
        assertThat(parsed, instanceOf(Invalid.class));
        return ((Invalid) parsed).violations().stream().map(Violation::toString).toList();
    }

    @Test
    public void validConfigurationKeepsValuesAsEntered() {
        ClaimToGroupsAndRolesConfig config = valid(config("claim", "entitlements ", "group.field.index", " 0 "));

        assertThat(config.claim(), equalTo("entitlements "));
        assertThat(config.groups().orElseThrow().fieldIndex(), equalTo(0));
        assertThat(config.roles().orElseThrow().table(),
                equalTo(Map.of("admin", List.of("tenant-admin "), " viewer", List.of("tenant-viewer"))));
        assertThat(config.stateFilter(), equalTo(Optional.empty()));
    }

    @Test
    public void missingOrBlankDelimiterDefaultsToComma() {
        Map<String, String> missing = config();
        missing.remove("delimiter");

        assertThat(valid(missing).delimiter(), equalTo(','));
        assertThat(valid(config("delimiter", " ")).delimiter(), equalTo(','));
        assertThat(valid(config("delimiter", ";")).delimiter(), equalTo(';'));
    }

    @Test
    public void invalidDelimiterIsRejected() {
        assertThat(violations(config("delimiter", ",;")), contains("'delimiter' must be exactly one visible character"));
        assertThat(violations(config("delimiter", "\u200b")), contains("'delimiter' must be exactly one visible character"));
    }

    @Test
    public void blankClaimIsRejected() {
        assertThat(violations(config("claim", " ")), contains("'claim' is required"));
    }

    @Test
    public void invalidIndexIsRejected() {
        assertThat(violations(config("group.field.index", "-1")), contains("'group.field.index' must be a whole number from 0 to 9999"));
        assertThat(violations(config("group.field.index", "10000")), contains("'group.field.index' must be a whole number from 0 to 9999"));
        assertThat(valid(config("group.field.index", "9999")).groups().orElseThrow().fieldIndex(), equalTo(9999));
    }

    @Test
    public void unpairedKeysAreRejected() {
        assertThat(violations(config("group", "")), contains("'group' is required when 'group.field.index' is set"));
        assertThat(violations(config("roles", "[]")), contains("'roles' is required when 'role.field.index' is set"));
        assertThat(violations(config("state.field.index", "2")), contains("'state.value' is required when 'state.field.index' is set"));
        assertThat(violations(config("state.value", "active")), contains("'state.field.index' is required when 'state.value' is set"));
    }

    @Test
    public void unreadableRoleTableIsTheOnlyViolation() {
        assertThat(violations(config("roles", "{not json")), contains("'roles' is not a readable list of field value and role pairs"));
    }

    @Test
    public void rowsWithoutFieldValueOrRoleAreDropped() {
        String table = "[{\"key\":\" \",\"value\":\"tenant-admin\"},{\"key\":\"viewer\",\"value\":\" \"},{\"key\":\"admin\",\"value\":\"tenant-admin\"}]";

        assertThat(valid(config("roles", table)).roles().orElseThrow().table(), equalTo(Map.of("admin", List.of("tenant-admin"))));
    }

    @Test
    public void noOutputIsRejected() {
        Map<String, String> raw = config("group.field.index", "", "role.field.index", "", "group", "", "roles", "");

        assertThat(violations(raw), contains("'group.field.index' and 'role.field.index' are both empty; set at least one"));
    }

    @Test
    public void rewritingRoleReferencesReplacesOnlyTheRoleTable() {
        Map<String, String> raw = config();

        Map<String, String> rewritten = ClaimToGroupsAndRolesConfig.withRewrittenRoleReferences(raw, String::toUpperCase);

        assertThat(ClaimToGroupsAndRolesConfig.roleReferences(rewritten), equalTo(Set.of("TENANT-ADMIN ", "TENANT-VIEWER")));
        assertThat(rewritten.get("claim"), equalTo(raw.get("claim")));
        assertThat(rewritten.size(), equalTo(raw.size()));
        assertThat(raw.get("roles"), equalTo(TABLE));
    }

    @Test
    public void unreadableRoleTableIsNotRewritten() {
        Map<String, String> raw = config("roles", "[null]");

        assertThat(ClaimToGroupsAndRolesConfig.withRewrittenRoleReferences(raw, String::toUpperCase), equalTo(raw));
    }

    @Test
    public void sharedIndexIsRejected() {
        assertThat(violations(config("role.field.index", "0")),
                contains("'group.field.index' and 'role.field.index' point at the same field"));
    }
}
