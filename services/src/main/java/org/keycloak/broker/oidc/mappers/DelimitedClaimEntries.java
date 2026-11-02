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
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.keycloak.broker.oidc.mappers.ClaimToGroupsAndRolesConfig.GroupOutput;
import org.keycloak.broker.oidc.mappers.ClaimToGroupsAndRolesConfig.RoleOutput;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Reads the entries of a claim whose value is a string, or a list of strings, of delimited fields.
 */
final class DelimitedClaimEntries {

    /**
     * Counted in UTF-16 code units, as the length validator of Keycloak counts.
     */
    static final int MAX_GROUP_NAME_LENGTH = 255;

    sealed interface Outcome permits Accepted, Skipped {
    }

    /**
     * @param groupName the name of the group under the parent group, when groups are mapped
     * @param roles   the role references the entry's role field grants; empty when the value is not in the table
     */
    record Accepted(Optional<String> groupName, List<String> roles) implements Outcome {
    }

    record Skipped(String entry, SkipReason reason) implements Outcome {
    }

    /**
     * Why an entry is skipped. A refused group name means an entitlement the user does not get although the entry has
     * the expected shape.
     */
    enum SkipReason {
        NOT_A_STRING("is not a string", false),
        MISSING_FIELD("has fewer fields than the configured field indexes need", false),
        OTHER_STATE("does not have the required state", false),
        EMPTY_GROUP_NAME("has an empty group name", true),
        LONG_GROUP_NAME("has a group name longer than " + MAX_GROUP_NAME_LENGTH + " characters", true),
        SLASH_IN_GROUP_NAME("has a group name containing '/'", true),
        BLANK_IN_GROUP_NAME("has a group name containing whitespace", true),
        INVISIBLE_IN_GROUP_NAME("has a group name containing a control or formatting character", true);

        private final String description;
        private final boolean refusedGroupName;

        SkipReason(String description, boolean refusedGroupName) {
            this.description = description;
            this.refusedGroupName = refusedGroupName;
        }

        boolean refusedGroupName() {
            return refusedGroupName;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    private record GroupNameCheck(Predicate<String> refuses, SkipReason reason) {
    }

    /**
     * Checked in order; the first that refuses a group name gives the reason.
     */
    private static final List<GroupNameCheck> GROUP_NAME_CHECKS = List.of(
            new GroupNameCheck(String::isEmpty, SkipReason.EMPTY_GROUP_NAME),
            new GroupNameCheck(DelimitedClaimEntries::isTooLong, SkipReason.LONG_GROUP_NAME),
            new GroupNameCheck(DelimitedClaimEntries::containsSlash, SkipReason.SLASH_IN_GROUP_NAME),
            new GroupNameCheck(DelimitedClaimEntries::containsBlank, SkipReason.BLANK_IN_GROUP_NAME),
            new GroupNameCheck(DelimitedClaimEntries::containsInvisible, SkipReason.INVISIBLE_IN_GROUP_NAME));

    record Mapping(List<Accepted> accepted, List<Skipped> skipped) {

        Set<String> groupNames() {
            return accepted.stream().map(Accepted::groupName).flatMap(Optional::stream)
                    .collect(Collectors.toUnmodifiableSet());
        }

        Set<String> roles() {
            return accepted.stream().map(Accepted::roles).flatMap(List::stream)
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    private DelimitedClaimEntries() {
    }

    /**
     * @param claimValue the claim; a text node is one entry, an array holds one entry per element
     */
    static Mapping read(Optional<JsonNode> claimValue, ClaimToGroupsAndRolesConfig config) {
        List<Outcome> outcomes = outcomes(claimValue.map(DelimitedClaimEntries::entries).orElse(List.of()), config);
        return new Mapping(outcomesOfType(outcomes, Accepted.class), outcomesOfType(outcomes, Skipped.class));
    }

    private static List<Outcome> outcomes(List<JsonNode> entries, ClaimToGroupsAndRolesConfig config) {
        return entries.stream().map(entry -> readItem(entry, config)).toList();
    }

    private static <T extends Outcome> List<T> outcomesOfType(List<Outcome> outcomes, Class<T> type) {
        return outcomes.stream().filter(type::isInstance).map(type::cast).toList();
    }

    private static List<JsonNode> entries(JsonNode claimValue) {
        return claimValue.isArray()
                ? StreamSupport.stream(claimValue.spliterator(), false).toList()
                : List.of(claimValue);
    }

    private static Outcome readItem(JsonNode entry, ClaimToGroupsAndRolesConfig config) {
        return entry.isTextual() ? readEntry(entry.textValue(), config) : notAString(entry);
    }

    private static Outcome notAString(JsonNode entry) {
        return new Skipped(entry.toString(), SkipReason.NOT_A_STRING);
    }

    private static Outcome readEntry(String entry, ClaimToGroupsAndRolesConfig config) {
        List<String> fields = List.of(entry.split(Pattern.quote(String.valueOf(config.delimiter())), -1));
        return skipReason(fields, config)
                .<Outcome>map(reason -> new Skipped(entry, reason))
                .orElseGet(() -> accepted(fields, config));
    }

    private static Optional<SkipReason> skipReason(List<String> fields, ClaimToGroupsAndRolesConfig config) {
        return missingField(fields, config)
                .or(() -> otherState(fields, config))
                .or(() -> entryGroupNameProblem(fields, config));
    }

    private static Accepted accepted(List<String> fields, ClaimToGroupsAndRolesConfig config) {
        return new Accepted(groupName(fields, config), grantedRoles(fields, config));
    }

    private static Optional<String> groupName(List<String> fields, ClaimToGroupsAndRolesConfig config) {
        return config.groups().map(GroupOutput::fieldIndex).map(fields::get);
    }

    private static Optional<SkipReason> entryGroupNameProblem(List<String> fields, ClaimToGroupsAndRolesConfig config) {
        return groupName(fields, config).flatMap(DelimitedClaimEntries::groupNameProblem);
    }

    private static Optional<SkipReason> missingField(List<String> fields, ClaimToGroupsAndRolesConfig config) {
        return fields.size() > config.highestFieldIndex() ? Optional.empty() : Optional.of(SkipReason.MISSING_FIELD);
    }

    private static Optional<SkipReason> otherState(List<String> fields, ClaimToGroupsAndRolesConfig config) {
        return config.stateFilter()
                .filter(state -> !state.requiredValue().equals(fields.get(state.fieldIndex())))
                .map(state -> SkipReason.OTHER_STATE);
    }

    private static List<String> grantedRoles(List<String> fields, ClaimToGroupsAndRolesConfig config) {
        return config.roles().map(roles -> grantedRoles(fields, roles)).orElse(List.of());
    }

    private static List<String> grantedRoles(List<String> fields, RoleOutput roles) {
        return roles.table().getOrDefault(fields.get(roles.fieldIndex()), List.of());
    }

    private static Optional<SkipReason> groupNameProblem(String groupName) {
        return GROUP_NAME_CHECKS.stream()
                .filter(check -> check.refuses().test(groupName))
                .findFirst()
                .map(GroupNameCheck::reason);
    }

    private static boolean isTooLong(String groupName) {
        return groupName.length() > MAX_GROUP_NAME_LENGTH;
    }

    private static boolean containsSlash(String groupName) {
        return groupName.indexOf('/') >= 0;
    }

    private static boolean containsBlank(String groupName) {
        return groupName.codePoints().anyMatch(DelimitedClaimEntries::isBlank);
    }

    private static boolean containsInvisible(String groupName) {
        return groupName.codePoints().anyMatch(DelimitedClaimEntries::isInvisible);
    }

    /**
     * Whitespace in the JDK sense or in the Unicode sense, which adds the no-break spaces.
     */
    static boolean isBlank(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    /**
     * Control and format characters (zero-width spaces and joiners, the byte order mark, bidirectional overrides),
     * and unpaired surrogates.
     */
    static boolean isInvisible(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.CONTROL || type == Character.FORMAT || type == Character.SURROGATE;
    }
}
