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

import org.keycloak.common.util.MultivaluedHashMap;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class MembershipTranslationTest {

    @Test
    public void patternMustMatchWholeName() {
        NameExpression expression = expression("myapp-(.+)-users", "$1");

        assertThat(expression.translate("myapp-Admin-users"), is(Optional.of("Admin")));
        assertThat(expression.translate("myapp-Report_Viewer-EU-users"), is(Optional.of("Report_Viewer-EU")));
        assertThat(expression.translate("X_myapp-Admin-users"), is(Optional.empty()));
        assertThat(expression.translate("myapp-Admin-users_old"), is(Optional.empty()));
    }

    @Test
    public void templateFillsNumberedAndNamedCapturesAndKeepsLiterals() {
        NameExpression expression = expression("(?<app>[A-Z]+)-(\\w+)", "/apps/${app}/$2 $");

        assertThat(expression.translate("CRM-editor"), is(Optional.of("/apps/CRM/editor $")));
    }

    @Test
    public void namedCaptureOfPatternWithTrailingComment() {
        NameExpression expression = expression("(?x)(?<role>.+) # role", "${role}");

        assertThat(expression.translate("Admin"), is(Optional.of("Admin")));
    }

    @Test
    public void emptyTranslationIsNoTranslation() {
        NameExpression expression = expression("prefix_(\\w*)", "$1");

        assertThat(expression.translate("prefix_"), is(Optional.empty()));
        assertThat(expression("prefix_(\\w*)", " $1").translate("prefix_"), is(Optional.empty()));
    }

    @Test
    public void namesAreKeptVerbatim() {
        Map<String, List<String>> table = table("[{\"key\":\" Admin\",\"value\":\" Admin role \"}]");
        MembershipTranslation translation = new MembershipTranslation(table, Optional.of(expression("app-(.+)", " $1 ")));

        assertThat(translation.translateAll(List.of(" Admin")).names(), contains(" Admin role "));
        assertThat(translation.translateAll(List.of("Admin")).untranslated(), contains("Admin"));
        assertThat(translation.translateAll(List.of("app-Viewer")).names(), contains(" Viewer "));
        assertThat(LDAPGroupNames.of(List.of(" Admin ", "cn=\\ Viewer,ou=groups", "cn= Editor ,ou=groups"), "cn").names(), containsInAnyOrder(" Admin ", " Viewer", "Editor"));
        assertThat(parse(Map.of(TranslatedMembershipConfig.TRANSLATION_TABLE, "[{\"key\":\"a\",\"value\":\"b\"}]",
                TranslatedMembershipConfig.TARGET, GrantTarget.CLIENT_ROLE,
                TranslatedMembershipConfig.CLIENT_ID, " app ")).value().orElseThrow().target(), equalTo(new GrantTarget.ClientRole(" app ")));
    }

    @Test
    public void logValuesAreSortedQuotedAndFreeOfControlCharacters() {
        assertThat(TranslatedMembershipLDAPStorageMapper.forLog(List.of("b\nWARN forged", "a\u001B[31m")), equalTo("'a', 'bWARN forged'"));
    }

    @Test
    public void tableWinsOverPattern() {
        MembershipTranslation translation = new MembershipTranslation(
                Map.of("myapp-Admin-users", List.of("SuperAdmin", "Auditor")),
                Optional.of(expression("myapp-(.+)-users", "$1")));

        MembershipTranslation.Translated translated = translation.translateAll(List.of("myapp-Admin-users", "myapp-User-users", "Other"));

        assertThat(translated.fromTable(), containsInAnyOrder("SuperAdmin", "Auditor"));
        assertThat(translated.fromExpression(), contains("User"));
        assertThat(translated.names(), containsInAnyOrder("SuperAdmin", "Auditor", "User"));
        assertThat(translated.untranslated(), contains("Other"));
    }

    @Test
    public void groupNameIsFirstRdnOfDnOrRawValue() {
        LDAPGroupNames names = LDAPGroupNames.of(
                List.of("cn=myapp-Admin-users,ou=groups,dc=example,dc=org", "myapp-User-users", "ou=nameless,dc=example,dc=org", ""),
                "cn");

        assertThat(names.names(), containsInAnyOrder("myapp-Admin-users", "myapp-User-users"));
        assertThat(names.unreadable(), contains("ou=nameless,dc=example,dc=org"));
    }

    @Test
    public void configDefaultsToMemberOfAndRealmRoles() {
        TranslatedMembershipConfig config = parse(Map.of(TranslatedMembershipConfig.TRANSLATION_TABLE, "[{\"key\":\"a\",\"value\":\"b\"}]")).value().orElseThrow();

        assertThat(config.membershipAttribute(), equalTo("memberOf"));
        assertThat(config.groupNameAttribute(), equalTo("cn"));
        assertThat(config.target(), equalTo(new GrantTarget.RealmRole()));
        assertThat(config.translation().translateAll(List.of("a")).names(), contains("b"));
    }

    @Test
    public void configRejectsWhatWouldGrantNothing() {
        assertThat(parse(Map.of()).problems(), contains(containsString("Configure a translation table")));
        assertThat(parse(Map.of(TranslatedMembershipConfig.TRANSLATION_PATTERN, "(.+)")).problems(), contains(containsString("Configure both or neither")));
        assertThat(expressionProblems("(.+", "$1"), contains(containsString("not a valid regular expression")));
        assertThat(expressionProblems("(.+)", "Admin"), contains(containsString("refers to no capture group")));
        assertThat(expressionProblems("(.+)", "$2"), contains(containsString("refers to $2, but the pattern has 1 capture group.")));
        assertThat(expressionProblems("(a)(b)", "$3"), contains(containsString("refers to $3, but the pattern has 2 capture groups.")));
        assertThat(expressionProblems("(.+)", "$2147483648"), contains(containsString("refers to $2147483648, but the pattern has 1 capture group.")));
        assertThat(expressionProblems("(?<app>.+)", "${role}"), contains(containsString("no capture group named 'role'")));
        assertThat(expressionProblems("(?<app>.+)", "${my_app}"), contains(containsString("refers to ${my_app}, but a capture group name consists of letters and digits")));
        assertThat(expressionProblems("(?<app>.+)", "${app"), contains(containsString("Translation template has an unclosed ${.")));
        assertThat(expressionProblems("(.+)", "${1}"), contains(containsString("refers to ${1}, but a capture group name consists of letters and digits")));
        assertThat(tableProblems("not json"), contains(containsString("not a list of key and value pairs")));
        assertThat(tableProblems("[{\"key\":\"a\",\"value\":\" \"}]"), contains(containsString("maps 'a' to a blank name")));
        assertThat(tableProblems("[{\"key\":\"a\",\"value\":null}]"), contains(containsString("not a list of key and value pairs")));
    }

    @Test
    public void configRequiresClientForClientRoles() {
        Map<String, String> clientRoles = Map.of(
                TranslatedMembershipConfig.TRANSLATION_TABLE, "[{\"key\":\"a\",\"value\":\"b\"}]",
                TranslatedMembershipConfig.TARGET, GrantTarget.CLIENT_ROLE);

        assertThat(parse(clientRoles).problems(), contains(containsString("Select the client")));
        assertThat(parse(Map.of(TranslatedMembershipConfig.TRANSLATION_TABLE, "[{\"key\":\"a\",\"value\":\"b\"}]", TranslatedMembershipConfig.TARGET, "ROLE")).problems(),
                contains(containsString("is not one of")));

        Parsed<TranslatedMembershipConfig> withClient = parse(Map.of(
                TranslatedMembershipConfig.TRANSLATION_TABLE, "[{\"key\":\"a\",\"value\":\"b\"}]",
                TranslatedMembershipConfig.TARGET, GrantTarget.CLIENT_ROLE,
                TranslatedMembershipConfig.CLIENT_ID, "app"));
        assertThat(withClient.problems(), is(empty()));
        assertThat(withClient.value().orElseThrow().target(), equalTo(new GrantTarget.ClientRole("app")));
    }

    @Test
    public void configReportsEveryProblemAtOnce() {
        assertThat(parse(Map.of(TranslatedMembershipConfig.TRANSLATION_TEMPLATE, "$1", TranslatedMembershipConfig.TARGET, GrantTarget.CLIENT_ROLE)).problems(), hasSize(2));
    }

    private static NameExpression expression(String pattern, String template) {
        Parsed<NameExpression> parsed = NameExpression.parse(pattern, template);
        assertThat(parsed.problems(), is(empty()));
        return parsed.value().orElseThrow();
    }

    private static List<String> expressionProblems(String pattern, String template) {
        return parse(Map.of(TranslatedMembershipConfig.TRANSLATION_PATTERN, pattern, TranslatedMembershipConfig.TRANSLATION_TEMPLATE, template)).problems();
    }

    private static List<String> tableProblems(String json) {
        return parse(Map.of(TranslatedMembershipConfig.TRANSLATION_TABLE, json)).problems();
    }

    private static Map<String, List<String>> table(String json) {
        return parse(Map.of(TranslatedMembershipConfig.TRANSLATION_TABLE, json)).value().orElseThrow().translation().table();
    }

    private static Parsed<TranslatedMembershipConfig> parse(Map<String, String> values) {
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        values.forEach(config::putSingle);
        return TranslatedMembershipConfig.parse(config);
    }
}
