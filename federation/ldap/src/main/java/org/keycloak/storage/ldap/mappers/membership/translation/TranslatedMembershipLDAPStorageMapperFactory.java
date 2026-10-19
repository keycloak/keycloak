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

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapperFactory;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

import static org.keycloak.provider.ProviderConfigProperty.CLIENT_LIST_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.LIST_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.MAP_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;
import static org.keycloak.storage.ldap.mappers.membership.translation.TranslatedMembershipConfig.CLIENT_ID;
import static org.keycloak.storage.ldap.mappers.membership.translation.TranslatedMembershipConfig.GROUP_NAME_ATTRIBUTE;
import static org.keycloak.storage.ldap.mappers.membership.translation.TranslatedMembershipConfig.MEMBERSHIP_ATTRIBUTE;
import static org.keycloak.storage.ldap.mappers.membership.translation.TranslatedMembershipConfig.TARGET;
import static org.keycloak.storage.ldap.mappers.membership.translation.TranslatedMembershipConfig.TARGETS;
import static org.keycloak.storage.ldap.mappers.membership.translation.TranslatedMembershipConfig.TRANSLATION_PATTERN;
import static org.keycloak.storage.ldap.mappers.membership.translation.TranslatedMembershipConfig.TRANSLATION_TABLE;
import static org.keycloak.storage.ldap.mappers.membership.translation.TranslatedMembershipConfig.TRANSLATION_TEMPLATE;

public class TranslatedMembershipLDAPStorageMapperFactory extends AbstractLDAPStorageMapperFactory {

    public static final String PROVIDER_ID = "translated-membership-ldap-mapper";

    private static final Logger logger = Logger.getLogger(TranslatedMembershipLDAPStorageMapperFactory.class);

    private static final String HELP_TEXT = "Grants realm roles, client roles or groups whose names are translated from the names of the LDAP groups the user is member of. Read-only: nothing is written to LDAP.";
    private static final String MEMBERSHIP_ATTRIBUTE_HELP = "Name of the LDAP attribute on the user listing the LDAP groups the user is member of. Usually 'memberOf'.";
    private static final String GROUP_NAME_ATTRIBUTE_HELP = "Attribute in the first RDN of a member-of value that holds the LDAP group name. Usually 'cn'. A value that is not a DN is used as the group name.";
    private static final String TRANSLATION_TABLE_HELP = "LDAP group name as the key, role name or full group path as the value. Exact and case sensitive. An entry wins over the translation pattern.";
    private static final String TRANSLATION_PATTERN_HELP = "Regular expression that must match the whole LDAP group name. Its capture groups are used in the translation template. For example 'myapp-(.+)-users'.";
    private static final String TRANSLATION_TEMPLATE_HELP = "Role name or full group path built from the capture groups of the translation pattern, referenced as $1 or ${name}. For example '$1' or '/applications/$1'.";
    private static final String TARGET_HELP = "What the translated names denote: realm roles, roles of the selected client, or groups by their full path.";
    private static final String CLIENT_ID_HELP = "Client whose roles are granted. Used only when the target is CLIENT_ROLE.";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
            .property().name(MEMBERSHIP_ATTRIBUTE).label("Member-Of LDAP Attribute").helpText(MEMBERSHIP_ATTRIBUTE_HELP).type(STRING_TYPE).defaultValue(LDAPConstants.MEMBER_OF).add()
            .property().name(GROUP_NAME_ATTRIBUTE).label("Group Name LDAP Attribute").helpText(GROUP_NAME_ATTRIBUTE_HELP).type(STRING_TYPE).defaultValue(LDAPConstants.CN).add()
            .property().name(TRANSLATION_TABLE).label("Translation Table").helpText(TRANSLATION_TABLE_HELP).type(MAP_TYPE).add()
            .property().name(TRANSLATION_PATTERN).label("Translation Pattern").helpText(TRANSLATION_PATTERN_HELP).type(STRING_TYPE).add()
            .property().name(TRANSLATION_TEMPLATE).label("Translation Template").helpText(TRANSLATION_TEMPLATE_HELP).type(STRING_TYPE).add()
            .property().name(TARGET).label("Target").helpText(TARGET_HELP).type(LIST_TYPE).options(TARGETS).defaultValue(GrantTarget.REALM_ROLE).add()
            .property().name(CLIENT_ID).label("Client ID").helpText(CLIENT_ID_HELP).type(CLIENT_LIST_TYPE).add()
            .build();

    @Override
    public String getHelpText() {
        return HELP_TEXT;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        List<String> problems = problemsOf(realm, TranslatedMembershipConfig.parse(config.getConfig()));
        if (!problems.isEmpty()) {
            throw new ComponentValidationException(asMessageFormatLiteral(String.join(" ", problems)));
        }
    }

    @Override
    protected AbstractLDAPStorageMapper createMapper(ComponentModel mapperModel, LDAPStorageProvider federationProvider) {
        Parsed<TranslatedMembershipConfig> parsed = TranslatedMembershipConfig.parse(mapperModel.getConfig());
        parsed.problems().forEach(problem -> warnGrantingNothing(mapperModel, problem));
        return new TranslatedMembershipLDAPStorageMapper(mapperModel, federationProvider, parsed.value().orElseGet(TranslatedMembershipConfig::grantingNothing));
    }

    private static List<String> problemsOf(RealmModel realm, Parsed<TranslatedMembershipConfig> parsed) {
        return parsed.check(config -> config.target().problemIn(realm)).problems();
    }

    private static void warnGrantingNothing(ComponentModel mapperModel, String problem) {
        logger.warnf("LDAP mapper '%s' grants nothing: %s", StringUtil.removeControlCharacters(mapperModel.getName()), StringUtil.removeControlCharacters(problem));
    }

    // The admin REST API renders validation messages with MessageFormat, and the problems quote patterns and templates
    private static String asMessageFormatLiteral(String text) {
        return text.replace("'", "''").replace("{", "'{'").replace("}", "'}'");
    }
}
