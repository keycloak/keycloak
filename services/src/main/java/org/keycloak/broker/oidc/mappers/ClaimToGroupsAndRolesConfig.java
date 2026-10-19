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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.utils.MapperTypeSerializer;

/**
 * Configuration of {@link ClaimToGroupsAndRolesMapper}. Instances are only created by {@link #parse(Map)} and are
 * internally consistent: every configured field index is distinct, and each output carries everything it needs.
 */
record ClaimToGroupsAndRolesConfig(String claim, char delimiter, Optional<StateFilter> stateFilter,
                                   Optional<GroupOutput> groups, Optional<RoleOutput> roles) {

    static final String CLAIM = AbstractClaimMapper.CLAIM;
    static final String DELIMITER = "delimiter";
    static final String DEFAULT_DELIMITER = ",";
    static final String GROUP_FIELD_INDEX = "group.field.index";
    static final String PARENT_GROUP = ConfigConstants.GROUP;
    static final String ROLE_FIELD_INDEX = "role.field.index";
    static final String ROLES = "roles";
    static final String STATE_FIELD_INDEX = "state.field.index";
    static final String STATE_VALUE = "state.value";

    record StateFilter(int fieldIndex, String requiredValue) {
    }

    record GroupOutput(int fieldIndex) {
    }

    /**
     * @param table field value to the role references it grants; a role reference is a realm role name or
     *              {@code clientId.roleName}
     */
    record RoleOutput(int fieldIndex, Map<String, List<String>> table) {

        Set<String> ownedRoles() {
            return references(table);
        }
    }

    /**
     * @param keys the configuration keys at fault
     */
    record Violation(List<String> keys, String problem) {

        static Violation of(String key, String problem) {
            return new Violation(List.of(key), problem);
        }

        @Override
        public String toString() {
            return keys.stream().map(key -> "'" + key + "'").collect(Collectors.joining(" and ")) + " " + problem;
        }
    }

    sealed interface Parsed permits Valid, Invalid {
    }

    record Valid(ClaimToGroupsAndRolesConfig config) implements Parsed {
    }

    record Invalid(List<Violation> violations) implements Parsed {
    }

    private static final Violation NO_OUTPUT =
            new Violation(List.of(GROUP_FIELD_INDEX, ROLE_FIELD_INDEX), "are both empty; set at least one");

    int highestFieldIndex() {
        return configuredFieldIndices().max(Integer::compare).orElse(0);
    }

    private Stream<Integer> configuredFieldIndices() {
        return Stream.of(stateFilter.map(StateFilter::fieldIndex), groups.map(GroupOutput::fieldIndex),
                        roles.map(RoleOutput::fieldIndex))
                .flatMap(Optional::stream);
    }

    static Parsed parse(Map<String, String> raw) {
        List<Violation> violations = Stream.of(
                        missingClaim(raw),
                        invalidDelimiter(raw),
                        invalidIndex(raw, GROUP_FIELD_INDEX),
                        invalidIndex(raw, ROLE_FIELD_INDEX),
                        invalidIndex(raw, STATE_FIELD_INDEX),
                        unpaired(raw, GROUP_FIELD_INDEX, PARENT_GROUP, text(raw, PARENT_GROUP).isPresent()),
                        unpaired(raw, ROLE_FIELD_INDEX, ROLES, roleTable(raw) != RoleTable.ABSENT),
                        unpaired(raw, STATE_FIELD_INDEX, STATE_VALUE, value(raw, STATE_VALUE).isPresent()),
                        unreadableRoleTable(raw),
                        noOutput(raw),
                        sharedIndex(raw))
                .flatMap(Optional::stream)
                .toList();
        return violations.isEmpty() ? new Valid(build(raw)) : new Invalid(violations);
    }

    /**
     * The role references in the role table, whether or not the rest of the configuration is valid; empty when the table
     * is absent or cannot be read.
     */
    static Set<String> roleReferences(Map<String, String> raw) {
        return usableRoleTable(raw).map(ClaimToGroupsAndRolesConfig::references).orElse(Set.of());
    }

    /**
     * The configuration with {@code rewriteReference} applied to each role reference in the role table; the given
     * configuration when no reference changes or the table cannot be read.
     */
    static Map<String, String> withRewrittenRoleReferences(Map<String, String> raw, UnaryOperator<String> rewriteReference) {
        return text(raw, ROLES)
                .flatMap(table -> RoleTableRows.rewritten(table, rewriteReference))
                .map(rewrittenTable -> withRoleTable(raw, rewrittenTable))
                .orElse(raw);
    }

    private static Map<String, String> withRoleTable(Map<String, String> raw, String table) {
        Map<String, String> config = new HashMap<>(raw);
        config.put(ROLES, table);
        return Collections.unmodifiableMap(config);
    }

    private static Set<String> references(Map<String, List<String>> table) {
        return table.values().stream().flatMap(List::stream).collect(Collectors.toUnmodifiableSet());
    }

    private static ClaimToGroupsAndRolesConfig build(Map<String, String> raw) {
        return new ClaimToGroupsAndRolesConfig(
                claim(raw).orElseThrow(),
                delimiter(raw).charAt(0),
                parseStateFilter(raw),
                index(raw, GROUP_FIELD_INDEX).map(GroupOutput::new),
                parseRoleOutput(raw));
    }

    private static Optional<StateFilter> parseStateFilter(Map<String, String> raw) {
        return index(raw, STATE_FIELD_INDEX)
                .map(fieldIndex -> new StateFilter(fieldIndex, value(raw, STATE_VALUE).orElseThrow()));
    }

    private static Optional<RoleOutput> parseRoleOutput(Map<String, String> raw) {
        return index(raw, ROLE_FIELD_INDEX)
                .map(fieldIndex -> new RoleOutput(fieldIndex, usableRoleTable(raw).orElseThrow()));
    }

    private static Optional<Violation> missingClaim(Map<String, String> raw) {
        return claim(raw).isPresent() ? Optional.empty() : Optional.of(Violation.of(CLAIM, "is required"));
    }

    private static Optional<String> claim(Map<String, String> raw) {
        return value(raw, CLAIM).filter(Predicate.not(String::isBlank));
    }

    /**
     * The configured delimiter, or {@link #DEFAULT_DELIMITER} when none is configured.
     */
    private static String delimiter(Map<String, String> raw) {
        return value(raw, DELIMITER).filter(Predicate.not(String::isBlank)).orElse(DEFAULT_DELIMITER);
    }

    private static Optional<Violation> invalidDelimiter(Map<String, String> raw) {
        String delimiter = delimiter(raw);
        return delimiter.length() == 1 && isVisible(delimiter.charAt(0))
                ? Optional.empty()
                : Optional.of(Violation.of(DELIMITER, "must be exactly one visible character"));
    }

    private static boolean isVisible(char character) {
        return !Character.isSurrogate(character)
                && !DelimitedClaimEntries.isBlank(character)
                && !DelimitedClaimEntries.isInvisible(character);
    }

    private static Optional<Violation> invalidIndex(Map<String, String> raw, String key) {
        return text(raw, key)
                .filter(text -> parseIndex(text).isEmpty())
                .map(text -> Violation.of(key, "must be a whole number from 0 to 9999"));
    }

    private static Optional<Violation> unpaired(Map<String, String> raw, String indexKey, String requiredKey,
                                                boolean hasRequired) {
        boolean hasIndex = text(raw, indexKey).isPresent();
        return hasIndex == hasRequired ? Optional.empty() : Optional.of(missingPartner(hasIndex, indexKey, requiredKey));
    }

    private static Violation missingPartner(boolean hasIndex, String indexKey, String requiredKey) {
        return hasIndex ? requiredWhenSet(requiredKey, indexKey) : requiredWhenSet(indexKey, requiredKey);
    }

    private static Violation requiredWhenSet(String missingKey, String setKey) {
        return Violation.of(missingKey, "is required when '" + setKey + "' is set");
    }

    private static Optional<Violation> unreadableRoleTable(Map<String, String> raw) {
        return roleTable(raw) == RoleTable.UNREADABLE
                ? Optional.of(Violation.of(ROLES, "is not a readable list of field value and role pairs"))
                : Optional.empty();
    }

    private static Optional<Violation> noOutput(Map<String, String> raw) {
        return text(raw, GROUP_FIELD_INDEX).isPresent() || text(raw, ROLE_FIELD_INDEX).isPresent()
                ? Optional.empty()
                : Optional.of(NO_OUTPUT);
    }

    private static Optional<Violation> sharedIndex(Map<String, String> raw) {
        return indexKeysByIndex(raw).values().stream()
                .filter(keys -> keys.size() > 1)
                .findFirst()
                .map(keys -> new Violation(List.copyOf(keys), "point at the same field"));
    }

    private static Map<Integer, List<String>> indexKeysByIndex(Map<String, String> raw) {
        return Stream.of(GROUP_FIELD_INDEX, ROLE_FIELD_INDEX, STATE_FIELD_INDEX)
                .flatMap(key -> indexOfKey(raw, key).stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    private static Optional<Map.Entry<Integer, String>> indexOfKey(Map<String, String> raw, String key) {
        return index(raw, key).map(fieldIndex -> Map.entry(fieldIndex, key));
    }

    private static Optional<Integer> index(Map<String, String> raw, String key) {
        return text(raw, key).flatMap(ClaimToGroupsAndRolesConfig::parseIndex);
    }

    private static Optional<Integer> parseIndex(String text) {
        return isIndexText(text) ? Optional.of(Integer.valueOf(text)) : Optional.empty();
    }

    private static boolean isIndexText(String text) {
        return text.chars().allMatch(ClaimToGroupsAndRolesConfig::isAsciiDigit) && text.length() <= 4;
    }

    private static boolean isAsciiDigit(int character) {
        return character >= '0' && character <= '9';
    }

    /**
     * How the role table is configured. A readable table without a usable row counts as absent.
     */
    private enum RoleTable {
        ABSENT,
        UNREADABLE,
        USABLE
    }

    private static RoleTable roleTable(Map<String, String> raw) {
        return text(raw, ROLES).map(ClaimToGroupsAndRolesConfig::classifyRoleTable).orElse(RoleTable.ABSENT);
    }

    private static RoleTable classifyRoleTable(String json) {
        return deserialize(json).map(ClaimToGroupsAndRolesConfig::classifyReadableRoleTable).orElse(RoleTable.UNREADABLE);
    }

    private static RoleTable classifyReadableRoleTable(Map<String, List<String>> table) {
        return usableRows(table).isEmpty() ? RoleTable.ABSENT : RoleTable.USABLE;
    }

    private static Optional<Map<String, List<String>>> usableRoleTable(Map<String, String> raw) {
        return text(raw, ROLES)
                .flatMap(ClaimToGroupsAndRolesConfig::deserialize)
                .map(ClaimToGroupsAndRolesConfig::usableRows)
                .filter(table -> !table.isEmpty());
    }

    /**
     * The rows with a non-blank field value, each with its non-blank role references; field values and role references
     * are kept as entered.
     */
    private static Map<String, List<String>> usableRows(Map<String, List<String>> table) {
        return table.entrySet().stream()
                .filter(row -> !row.getKey().isBlank())
                .map(row -> Map.entry(row.getKey(), nonBlank(row.getValue())))
                .filter(row -> !row.getValue().isEmpty())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static List<String> nonBlank(List<String> roleReferences) {
        return roleReferences.stream().filter(Predicate.not(String::isBlank)).toList();
    }

    private static Optional<Map<String, List<String>>> deserialize(String json) {
        try {
            return Optional.of(MapperTypeSerializer.deserialize(json));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static Optional<String> text(Map<String, String> raw, String key) {
        return value(raw, key).map(String::trim).filter(Predicate.not(String::isEmpty));
    }

    /**
     * The configured value as entered, without trimming, for values matched exactly against claims and model names.
     */
    private static Optional<String> value(Map<String, String> raw, String key) {
        return Optional.ofNullable(raw.get(key)).filter(Predicate.not(String::isEmpty));
    }
}
