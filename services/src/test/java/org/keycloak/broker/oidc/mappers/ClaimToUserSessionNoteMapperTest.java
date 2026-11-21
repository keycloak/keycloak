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

package org.keycloak.broker.oidc.mappers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ClaimToUserSessionNoteMapperTest {

    private final ClaimToUserSessionNoteMapper mapper = new ClaimToUserSessionNoteMapper();

    @Test
    public void storesMatchingStringClaim() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"department": "sales"}""");

        importNewUser(context, "department", "sales", false);

        assertEquals("sales", sessionNote(context, "department"));
    }

    @Test
    public void storesArrayClaimJoinedWithCommaWhenAnyValueMatches() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"groups": ["admin", "users"]}""");

        importNewUser(context, "groups", "users", false);

        assertEquals("admin,users", sessionNote(context, "groups"));
    }

    @Test
    public void storesStringClaimMatchingRegex() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"department": "sales"}""");

        importNewUser(context, "department", "sa.*", true);

        assertEquals("sales", sessionNote(context, "department"));
    }

    @Test
    public void regexRequiresAFullMatch() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"department": "sales"}""");

        importNewUser(context, "department", "sa", true);

        assertNull(sessionNote(context, "department"));
    }

    @Test
    public void regexDoesNotMatchNonStringClaims() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"groups": ["admin", "users"], "address": {"locality": "London"}, "level": 42}""");

        importNewUser(context, "groups", ".*", true);
        importNewUser(context, "address", ".*", true);
        importNewUser(context, "level", ".*", true);

        assertNull(sessionNote(context, "groups"));
        assertNull(sessionNote(context, "address"));
        assertNull(sessionNote(context, "level"));
    }

    @Test
    public void doesNotStoreArrayClaimWhenNoValueMatches() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"groups": ["admin", "users"]}""");

        importNewUser(context, "groups", "guests", false);

        assertNull(sessionNote(context, "groups"));
    }

    @Test
    public void storesObjectClaimAsJson() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"address": {"locality": "London"}}""");

        importNewUser(context, "address", "{\"locality\":\"London\"}", false);

        assertEquals("{\"locality\":\"London\"}", sessionNote(context, "address"));
    }

    @Test
    public void storesNestedObjectsInArrayAsJson() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"roles": [{"name": "admin"}, "user", null]}""");

        importNewUser(context, "roles", "user", false);

        assertEquals("{\"name\":\"admin\"},user", sessionNote(context, "roles"));
    }

    @Test
    public void storesNumberAndBooleanClaimsAsText() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"level": 42, "verified": true}""");

        importNewUser(context, "level", "42", false);
        importNewUser(context, "verified", "true", false);

        assertEquals("42", sessionNote(context, "level"));
        assertEquals("true", sessionNote(context, "verified"));
    }

    @Test
    public void missingOrEmptyClaimSetsNoNote() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"groups": []}""");

        importNewUser(context, "groups", "admin", false);
        importNewUser(context, "phone", "12345", false);

        assertNull(sessionNote(context, "groups"));
        assertNull(sessionNote(context, "phone"));
    }

    @Test
    public void emptyValueStoresStringClaim() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"department": "sales"}""");

        importNewUser(context, "department", "", false);

        assertEquals("sales", sessionNote(context, "department"));
    }

    @Test
    public void emptyValueStoresArrayClaimJoinedWithComma() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"amr": ["pwd", "mfa"]}""");

        importNewUser(context, "amr", "", false);

        assertEquals("pwd,mfa", sessionNote(context, "amr"));
    }

    @Test
    public void emptyValueStoresSingleElementArrayClaim() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"amr": ["otp"]}""");

        importNewUser(context, "amr", "", false);

        assertEquals("otp", sessionNote(context, "amr"));
    }

    @Test
    public void emptyValueStoresObjectClaimAsJson() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"address": {"locality": "London"}}""");

        importNewUser(context, "address", "", false);

        assertEquals("{\"locality\":\"London\"}", sessionNote(context, "address"));
    }

    @Test
    public void emptyValueStoresNestedClaim() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"address": {"locality": "London"}}""");

        importNewUser(context, "address.locality", "", false);

        assertEquals("London", sessionNote(context, "address.locality"));
    }

    @Test
    public void emptyValueStoresNumberAndBooleanClaimsAsText() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"level": 42, "verified": true}""");

        importNewUser(context, "level", "", false);
        importNewUser(context, "verified", "", false);

        assertEquals("42", sessionNote(context, "level"));
        assertEquals("true", sessionNote(context, "verified"));
    }

    @Test
    public void emptyValueStoresStringClaimWithRegexEnabled() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"department": "sales"}""");

        importNewUser(context, "department", "", true);

        assertEquals("sales", sessionNote(context, "department"));
    }

    @Test
    public void emptyValueStoresNonStringClaimsWithRegexEnabled() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"amr": ["pwd", "mfa"], "address": {"locality": "London"}, "level": 42}""");

        importNewUser(context, "amr", "", true);
        importNewUser(context, "address", "", true);
        importNewUser(context, "level", "", true);

        assertEquals("pwd,mfa", sessionNote(context, "amr"));
        assertEquals("{\"locality\":\"London\"}", sessionNote(context, "address"));
        assertEquals("42", sessionNote(context, "level"));
    }

    @Test
    public void blankValueIsTreatedAsEmpty() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"amr": ["pwd", "mfa"]}""");

        importNewUser(context, "amr", "  ", false);

        assertEquals("pwd,mfa", sessionNote(context, "amr"));
    }

    @Test
    public void emptyValueSetsNoNoteForMissingOrEmptyClaim() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"amr": [], "nothing": null}""");

        importNewUser(context, "amr", "", false);
        importNewUser(context, "nothing", "", false);
        importNewUser(context, "phone", "", false);

        assertNull(sessionNote(context, "amr"));
        assertNull(sessionNote(context, "nothing"));
        assertNull(sessionNote(context, "phone"));
    }

    @Test
    public void emptyValueStoresClaimEvenWhenOtherValuesDoNotMatch() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"amr": ["otp"]}""");

        mapper.importNewUser(null, null, null,
                model(List.of(entry("amr", "pwd"), entry("amr", "")), false), context);

        assertEquals("otp", sessionNote(context, "amr"));
    }

    @Test
    public void emptyValueOnlyAppliesToItsOwnClaim() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"amr": ["pwd", "mfa"], "department": "sales"}""");

        mapper.importNewUser(null, null, null,
                model(List.of(entry("amr", ""), entry("department", "hr")), false), context);

        assertEquals("pwd,mfa", sessionNote(context, "amr"));
        assertNull(sessionNote(context, "department"));
    }

    @Test
    public void keepsNullPropertiesOfObjectClaims() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"address": {"locality": "London", "region": null}}""");

        importNewUser(context, "address", "", false);

        assertEquals("{\"locality\":\"London\",\"region\":null}", sessionNote(context, "address"));
    }

    @Test
    public void matchesObjectClaimWithNullProperties() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"address": {"locality": "London", "region": null}}""");

        importNewUser(context, "address", "{\"locality\":\"London\",\"region\":null}", false);

        assertEquals("{\"locality\":\"London\",\"region\":null}", sessionNote(context, "address"));
    }

    @Test
    public void storesMixedArrayClaimFromUserInfo() throws Exception {
        BrokeredIdentityContext context = contextWithUserInfo("""
                {"roles": [{"name": "admin"}, "user", null]}""");

        importNewUser(context, "roles", "user", false);

        assertEquals("{\"name\":\"admin\"},user", sessionNote(context, "roles"));
    }

    @Test
    public void storesNumericArrayClaimFromUserInfo() throws Exception {
        BrokeredIdentityContext context = contextWithUserInfo("""
                {"levels": [1, 2]}""");

        importNewUser(context, "levels", "", false);

        assertEquals("1,2", sessionNote(context, "levels"));
    }

    @Test
    public void storesObjectClaimFromUserInfoAsJson() throws Exception {
        BrokeredIdentityContext context = contextWithUserInfo("""
                {"address": {"locality": "London", "region": null}}""");

        importNewUser(context, "address", "", false);

        assertEquals("{\"locality\":\"London\",\"region\":null}", sessionNote(context, "address"));
    }

    @Test
    public void trimsStringClaimFromUserInfoAsBefore() throws Exception {
        BrokeredIdentityContext context = contextWithUserInfo("""
                {"department": " sales ", "blank": "  "}""");

        importNewUser(context, "department", "sales", false);
        importNewUser(context, "blank", "", false);

        assertEquals("sales", sessionNote(context, "department"));
        assertNull(sessionNote(context, "blank"));
    }

    @Test
    public void doesNotStoreMissingClaimBelowArrayFromUserInfo() throws Exception {
        BrokeredIdentityContext context = contextWithUserInfo("""
                {"roles": [{"name": "admin"}]}""");

        importNewUser(context, "roles.missing", "", false);

        assertNull(sessionNote(context, "roles.missing"));
    }

    @Test
    public void doesNotStoreMissingIndexedClaimBelowArrayFromUserInfo() throws Exception {
        BrokeredIdentityContext context = contextWithUserInfo("""
                {"roles": [{"name": "admin"}]}""");

        importNewUser(context, "roles.missing[0]", "", false);

        assertNull(sessionNote(context, "roles.missing[0]"));
    }

    @Test
    public void doesNotStoreMissingIndexedClaimFromUserInfo() throws Exception {
        BrokeredIdentityContext context = contextWithUserInfo("""
                {"department": "sales"}""");

        importNewUser(context, "missing[0]", "", false);

        assertNull(sessionNote(context, "missing[0]"));
    }

    @Test
    public void storesArrayClaimNestedInObjectFromUserInfo() throws Exception {
        BrokeredIdentityContext context = contextWithUserInfo("""
                {"realm_access": {"roles": ["admin", 1]}}""");

        importNewUser(context, "realm_access.roles", "", false);

        assertEquals("admin,1", sessionNote(context, "realm_access.roles"));
    }

    @Test
    public void storesArrayElementByIndexFromUserInfo() throws Exception {
        BrokeredIdentityContext context = contextWithUserInfo("""
                {"roles": [{"name": "admin"}, {"name": "user"}]}""");

        importNewUser(context, "roles[1]", "", false);
        importNewUser(context, "roles[0].name", "", false);

        assertEquals("{\"name\":\"user\"}", sessionNote(context, "roles[1]"));
        assertEquals("admin", sessionNote(context, "roles[0].name"));
    }

    @Test
    public void prefersTokenClaimOverUserInfo() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"amr": ["pwd"]}""");
        context.getContextData().put(OIDCIdentityProvider.USER_INFO,
                JsonSerialization.readValue("""
                        {"amr": ["otp", 1]}""", JsonNode.class));

        importNewUser(context, "amr", "", false);

        assertEquals("pwd", sessionNote(context, "amr"));
    }

    private void importNewUser(BrokeredIdentityContext context, String claim, String value, boolean regex) {
        mapper.importNewUser(null, null, null, model(claim, value, regex), context);
    }

    private static IdentityProviderMapperModel model(String claim, String value, boolean regex) {
        return model(List.of(entry(claim, value)), regex);
    }

    private static Map<String, String> entry(String claim, String value) {
        return Map.of("key", claim, "value", value);
    }

    private static IdentityProviderMapperModel model(List<Map<String, String>> claims, boolean regex) {
        IdentityProviderMapperModel model = new IdentityProviderMapperModel();
        model.setName("test-mapper");
        try {
            model.setConfig(Map.of(
                    "claims", JsonSerialization.writeValueAsString(claims),
                    "are.claim.values.regex", Boolean.toString(regex)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return model;
    }

    // Without an authentication session the context stores session notes in its context data.
    @SuppressWarnings("unchecked")
    private static String sessionNote(BrokeredIdentityContext context, String name) {
        Map<String, String> notes =
                (Map<String, String>) context.getContextData().get(Constants.MAPPER_SESSION_NOTES);
        return notes == null ? null : notes.get(name);
    }

    private static BrokeredIdentityContext contextWithUserInfo(String userInfoJson) throws Exception {
        BrokeredIdentityContext context = emptyContext();
        context.getContextData().put(OIDCIdentityProvider.USER_INFO,
                JsonSerialization.readValue(userInfoJson, JsonNode.class));
        return context;
    }

    private static BrokeredIdentityContext emptyContext() {
        OIDCIdentityProviderConfig config = new OIDCIdentityProviderConfig();
        config.setAlias("oidc");
        config.setEnabled(true);
        return new BrokeredIdentityContext("test-user", config);
    }

    private static BrokeredIdentityContext contextWithClaims(String claimsJson) throws Exception {
        JsonWebToken token = JsonSerialization.readValue(claimsJson, JsonWebToken.class);
        BrokeredIdentityContext context = emptyContext();
        context.getContextData().put(OIDCIdentityProvider.VALIDATED_ACCESS_TOKEN, token);
        return context;
    }
}
