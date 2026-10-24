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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.keycloak.broker.oidc.mappers.ClaimToGroupsAndRolesConfig.Valid;
import org.keycloak.broker.oidc.mappers.DelimitedClaimEntries.Accepted;
import org.keycloak.broker.oidc.mappers.DelimitedClaimEntries.Mapping;
import org.keycloak.broker.oidc.mappers.DelimitedClaimEntries.SkipReason;
import org.keycloak.broker.oidc.mappers.DelimitedClaimEntries.Skipped;
import org.keycloak.util.JsonSerialization;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

public class DelimitedClaimEntriesTest {

    private static final ClaimToGroupsAndRolesConfig CONFIG = ((Valid) ClaimToGroupsAndRolesConfig.parse(Map.of(
            "claim", "entitlements",
            "delimiter", ",",
            "group.field.index", "0",
            "group", "/tenants",
            "role.field.index", "1",
            "roles", "[{\"key\":\"admin\",\"value\":\"tenant-admin\"}]",
            "state.field.index", "2",
            "state.value", "active"))).config();

    private static Mapping read(String json) {
        try {
            return DelimitedClaimEntries.read(Optional.of(JsonSerialization.mapper.readTree(json)), CONFIG);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static List<SkipReason> reasons(String json) {
        return read(json).skipped().stream().map(Skipped::reason).toList();
    }

    private static List<String> skippedEntries(Mapping mapping) {
        return mapping.skipped().stream().map(Skipped::entry).toList();
    }

    private static List<SkipReason> distinctReasons(Mapping mapping) {
        return mapping.skipped().stream().map(Skipped::reason).distinct().toList();
    }

    private static List<SkipReason> refusedGroupNameReasons() {
        return Arrays.stream(SkipReason.values()).filter(SkipReason::refusedGroupName).toList();
    }

    @Test
    public void acceptedEntryNamesItsGroupAndRoles() {
        Mapping mapping = read("[\"acme,admin,active\", \"globex,unknown,active\"]");

        assertThat(mapping.accepted(), contains(
                new Accepted(Optional.of("acme"), List.of("tenant-admin")),
                new Accepted(Optional.of("globex"), List.of())));
        assertThat(mapping.skipped(), empty());
        assertThat(mapping.roles(), equalTo(Set.of("tenant-admin")));
    }

    @Test
    public void singleStringIsOneEntry() {
        assertThat(read("\"acme,admin,active\"").groupNames(), equalTo(Set.of("acme")));
    }

    @Test
    public void absentClaimMapsNothing() {
        Mapping mapping = DelimitedClaimEntries.read(Optional.empty(), CONFIG);

        assertThat(mapping.accepted(), empty());
        assertThat(mapping.skipped(), empty());
    }

    @Test
    public void nonStringItemsAreSkipped() {
        Mapping mapping = read("[5, {\"a\":1}, null, true, \"acme,admin,active\"]");

        assertThat(skippedEntries(mapping), contains("5", "{\"a\":1}", "null", "true"));
        assertThat(distinctReasons(mapping), contains(SkipReason.NOT_A_STRING));
        assertThat(mapping.groupNames(), equalTo(Set.of("acme")));
    }

    @Test
    public void entryWithTooFewFieldsIsSkipped() {
        assertThat(reasons("[\"acme,admin\"]"), contains(SkipReason.MISSING_FIELD));
    }

    @Test
    public void entryWithOtherStateIsSkipped() {
        assertThat(reasons("[\"acme,admin,suspended\", \"acme,admin,active \"]"), contains(SkipReason.OTHER_STATE, SkipReason.OTHER_STATE));
    }

    @Test
    public void eachRefusedGroupNameHasItsReason() {
        String longName = "a".repeat(DelimitedClaimEntries.MAX_GROUP_NAME_LENGTH + 1);
        String json = "[\",admin,active\", \"" + longName + ",admin,active\", \"a/b,admin,active\", \" acme,admin,active\","
                + " \"no-break\u00a0space,admin,active\", \"zero\u200bwidth,admin,active\", \"bidi\u202eoverride,admin,active\"]";

        assertThat(reasons(json), contains(SkipReason.EMPTY_GROUP_NAME, SkipReason.LONG_GROUP_NAME, SkipReason.SLASH_IN_GROUP_NAME,
                SkipReason.BLANK_IN_GROUP_NAME, SkipReason.BLANK_IN_GROUP_NAME, SkipReason.INVISIBLE_IN_GROUP_NAME,
                SkipReason.INVISIBLE_IN_GROUP_NAME));
        assertThat(read(json).accepted(), empty());
    }

    @Test
    public void groupNameOfMaximumLengthIsAccepted() {
        String name = "a".repeat(DelimitedClaimEntries.MAX_GROUP_NAME_LENGTH);

        assertThat(read("[\"" + name + ",admin,active\"]").groupNames(), equalTo(Set.of(name)));
    }

    @Test
    public void onlyGroupNameProblemsAreRefusedGroupNames() {
        assertThat(refusedGroupNameReasons(), contains(
                SkipReason.EMPTY_GROUP_NAME, SkipReason.LONG_GROUP_NAME, SkipReason.SLASH_IN_GROUP_NAME, SkipReason.BLANK_IN_GROUP_NAME,
                SkipReason.INVISIBLE_IN_GROUP_NAME));
    }
}
