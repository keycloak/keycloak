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
package org.keycloak.storage.ldap.mappers.membership.translation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.utils.MapperTypeSerializer;
import org.keycloak.storage.ldap.mappers.membership.CommonLDAPGroupMapperConfig;
import org.keycloak.storage.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.storage.ldap.mappers.membership.role.RoleMapperConfig;

/**
 * The configuration of a {@link TranslatedMembershipLDAPStorageMapper}.
 */
public record TranslatedMembershipConfig(String membershipAttribute, String groupNameAttribute, MembershipTranslation translation, GrantTarget target) {

    public static final String MEMBERSHIP_ATTRIBUTE = CommonLDAPGroupMapperConfig.MEMBEROF_LDAP_ATTRIBUTE;
    public static final String GROUP_NAME_ATTRIBUTE = GroupMapperConfig.GROUP_NAME_LDAP_ATTRIBUTE;
    public static final String TRANSLATION_TABLE = "translation.table";
    public static final String TRANSLATION_PATTERN = "translation.pattern";
    public static final String TRANSLATION_TEMPLATE = "translation.template";
    public static final String TARGET = "target";
    public static final String CLIENT_ID = RoleMapperConfig.CLIENT_ID;

    public static final List<String> TARGETS = List.of(GrantTarget.REALM_ROLE, GrantTarget.CLIENT_ROLE, GrantTarget.GROUP);

    private static final String NO_TRANSLATION = "Configure a translation table, a translation pattern with a template, or both. Without them the mapper grants nothing.";
    private static final String INCOMPLETE_EXPRESSION = "A translation pattern and a translation template work together. Configure both or neither.";
    private static final String TABLE_NOT_PAIRS = "The translation table is not a list of key and value pairs.";
    private static final String TABLE_ENTRY_WITHOUT_GROUP_NAME = "The translation table has an entry without an LDAP group name.";

    static Parsed<TranslatedMembershipConfig> parse(MultivaluedHashMap<String, String> config) {
        return Parsed.combine(translation(config), target(config), (translation, target) -> withAttributes(config, translation, target));
    }

    /**
     * A configuration under which the mapper grants nothing.
     */
    static TranslatedMembershipConfig grantingNothing() {
        return new TranslatedMembershipConfig(LDAPConstants.MEMBER_OF, LDAPConstants.CN, new MembershipTranslation(Map.of(), Optional.empty()), new GrantTarget.RealmRole());
    }

    private static TranslatedMembershipConfig withAttributes(MultivaluedHashMap<String, String> config, MembershipTranslation translation, GrantTarget target) {
        return new TranslatedMembershipConfig(
                text(config, MEMBERSHIP_ATTRIBUTE).orElse(LDAPConstants.MEMBER_OF),
                text(config, GROUP_NAME_ATTRIBUTE).orElse(LDAPConstants.CN),
                translation,
                target);
    }

    private static Parsed<GrantTarget> target(MultivaluedHashMap<String, String> config) {
        String target = text(config, TARGET).orElse(GrantTarget.REALM_ROLE);
        return switch (target) {
            case GrantTarget.REALM_ROLE -> Parsed.valid(new GrantTarget.RealmRole());
            case GrantTarget.CLIENT_ROLE -> clientRoleTarget(config);
            case GrantTarget.GROUP -> Parsed.valid(new GrantTarget.Group());
            default -> Parsed.invalid("Target '" + target + "' is not one of " + String.join(", ", TARGETS) + ".");
        };
    }

    private static Parsed<GrantTarget> clientRoleTarget(MultivaluedHashMap<String, String> config) {
        return Parsed.required(text(config, CLIENT_ID), "Select the client whose roles are granted.").map(GrantTarget.ClientRole::new);
    }

    private static Parsed<MembershipTranslation> translation(MultivaluedHashMap<String, String> config) {
        return Parsed.combine(table(config), expression(config), MembershipTranslation::new)
                .flatMap(TranslatedMembershipConfig::requireSomeTranslation);
    }

    private static Parsed<MembershipTranslation> requireSomeTranslation(MembershipTranslation translation) {
        return translation.table().isEmpty() && translation.expression().isEmpty() ? Parsed.invalid(NO_TRANSLATION) : Parsed.valid(translation);
    }

    private static Parsed<Map<String, List<String>>> table(MultivaluedHashMap<String, String> config) {
        return text(config, TRANSLATION_TABLE)
                .map(TranslatedMembershipConfig::deserializeTable)
                .orElseGet(() -> Parsed.valid(Map.of()));
    }

    private static Parsed<Map<String, List<String>>> deserializeTable(String json) {
        return readTable(json)
                .map(TranslatedMembershipConfig::checkTable)
                .orElseGet(() -> Parsed.invalid(TABLE_NOT_PAIRS));
    }

    private static Optional<Map<String, List<String>>> readTable(String json) {
        try {
            return Optional.of(MapperTypeSerializer.deserialize(json));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static Parsed<Map<String, List<String>>> checkTable(Map<String, List<String>> table) {
        return Parsed.ofProblems(table, table.entrySet().stream().flatMap(TranslatedMembershipConfig::entryProblems).toList());
    }

    private static Stream<String> entryProblems(Map.Entry<String, List<String>> entry) {
        return Stream.concat(missingGroupName(entry.getKey()).stream(), blankTranslation(entry.getKey(), entry.getValue()).stream());
    }

    private static Optional<String> missingGroupName(String ldapGroupName) {
        return ldapGroupName.isBlank() ? Optional.of(TABLE_ENTRY_WITHOUT_GROUP_NAME) : Optional.empty();
    }

    private static Optional<String> blankTranslation(String ldapGroupName, List<String> names) {
        return names.stream().anyMatch(String::isBlank)
                ? Optional.of("The translation table maps '" + ldapGroupName + "' to a blank name.")
                : Optional.empty();
    }

    private static Parsed<Optional<NameExpression>> expression(MultivaluedHashMap<String, String> config) {
        Optional<String> pattern = text(config, TRANSLATION_PATTERN);
        Optional<String> template = text(config, TRANSLATION_TEMPLATE);
        return pattern.isEmpty() && template.isEmpty() ? Parsed.valid(Optional.empty()) : completeExpression(pattern, template);
    }

    private static Parsed<Optional<NameExpression>> completeExpression(Optional<String> pattern, Optional<String> template) {
        return Parsed.combine(Parsed.required(pattern, INCOMPLETE_EXPRESSION), Parsed.required(template, INCOMPLETE_EXPRESSION), NameExpression::parse)
                .flatMap(parsed -> parsed.map(Optional::of));
    }

    private static Optional<String> text(MultivaluedHashMap<String, String> config, String key) {
        return Optional.ofNullable(config.getFirst(key)).filter(value -> !value.isBlank());
    }
}
