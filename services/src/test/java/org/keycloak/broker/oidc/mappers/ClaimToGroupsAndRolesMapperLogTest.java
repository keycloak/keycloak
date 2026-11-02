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

import java.util.List;
import java.util.Optional;

import org.keycloak.broker.oidc.mappers.DelimitedClaimEntries.SkipReason;
import org.keycloak.broker.oidc.mappers.DelimitedClaimEntries.Skipped;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ClaimToGroupsAndRolesMapperLogTest {

    @Test
    public void lineBreaksBecomeSpaces() {
        String entry = "acme" + Character.toString(0x0a) + "WARN forged" + Character.toString(0x0d) + Character.toString(0x2028) + "line";
        assertThat(ClaimToGroupsAndRolesMapper.quoted(entry), equalTo("'acme WARN forged  line'"));
    }

    @Test
    public void quotesAreEscaped() {
        assertThat(ClaimToGroupsAndRolesMapper.quoted("it's"), equalTo("'it\\'s'"));
    }

    @Test
    public void otherControlAndFormatCharactersAreEscaped() {
        String entry = "a" + Character.toString(0x00) + "b" + Character.toString(0x1b) + "[31mc" + Character.toString(0x85) + "d" + Character.toString(0x202e) + "e";
        assertThat(ClaimToGroupsAndRolesMapper.quoted(entry), equalTo("'a\\u0000b\\u001b[31mc\\u0085d\\u202ee'"));
    }

    @Test
    public void backslashesAreEscapedBeforeQuotes() {
        assertThat(ClaimToGroupsAndRolesMapper.quoted("a\\b'"), equalTo("'a\\\\b\\''"));
    }

    @Test
    public void longValuesAreCut() {
        String value = "a".repeat(ClaimToGroupsAndRolesMapper.MAX_LOGGED_LENGTH) + "b";

        assertThat(ClaimToGroupsAndRolesMapper.quoted(value),
                equalTo("'" + "a".repeat(ClaimToGroupsAndRolesMapper.MAX_LOGGED_LENGTH) + "...'"));
        assertThat(ClaimToGroupsAndRolesMapper.quoted(value.substring(1)), equalTo("'" + value.substring(1) + "'"));
    }

    @Test
    public void printableTextIsKept() {
        assertThat(ClaimToGroupsAndRolesMapper.quoted("acme,admin,active " + Character.toString(0x017c)), equalTo("'acme,admin,active " + Character.toString(0x017c) + "'"));
    }

    @Test
    public void refusedGroupNamesAreSummarized() {
        List<Skipped> skipped = List.of(
                new Skipped("a/b,admin,active", SkipReason.SLASH_IN_GROUP_NAME),
                new Skipped("x,admin", SkipReason.MISSING_FIELD),
                new Skipped(" c,admin,active", SkipReason.BLANK_IN_GROUP_NAME),
                new Skipped(",admin,active", SkipReason.EMPTY_GROUP_NAME),
                new Skipped("d/e,admin,active", SkipReason.SLASH_IN_GROUP_NAME));

        assertThat(ClaimToGroupsAndRolesMapper.refusedGroupNamesWarning(skipped), equalTo(Optional.of(
                "skipped 4 claim entries because their group name cannot be used, so the user does not get their groups and roles: "
                        + "'a/b,admin,active' has a group name containing '/'; ' c,admin,active' has a group name containing whitespace; "
                        + "',admin,active' has an empty group name and 1 more. Correct the entries at the identity provider or the group field index.")));
    }

    @Test
    public void noSummaryWithoutRefusedGroupNames() {
        assertThat(ClaimToGroupsAndRolesMapper.refusedGroupNamesWarning(List.of(
                new Skipped("x,admin", SkipReason.MISSING_FIELD), new Skipped("5", SkipReason.NOT_A_STRING))), equalTo(Optional.empty()));
    }
}
