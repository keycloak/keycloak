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

package org.keycloak.broker.oid4vp.mappers;

import java.util.List;
import java.util.Map;

import org.keycloak.broker.oid4vp.OID4VPIdentityProvider;
import org.keycloak.broker.oid4vp.OID4VPIdentityProviderConfig;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Covers value conversion and target application. Claim path semantics are covered by
 * {@link ClaimPathTest}.
 */
public class OID4VPSdJwtUserAttributeMapperTest {

    private final OID4VPSdJwtUserAttributeMapper mapper = new OID4VPSdJwtUserAttributeMapper();

    @Test
    public void mapsStringClaimToSingleValuedAttribute() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"email": "alice@email.cz"}""");

        preprocess(context, "email", "emailAttribute");

        assertEquals("alice@email.cz", context.getUserAttribute("emailAttribute"));
    }

    @Test
    public void mapsNumberAndBooleanClaimsAsText() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"age": 42, "adult": true}""");

        preprocess(context, "age", "age");
        preprocess(context, "adult", "adult");

        assertEquals("42", context.getUserAttribute("age"));
        assertEquals("true", context.getUserAttribute("adult"));
    }

    @Test
    public void mapsArrayOfStringsToMultivaluedAttribute() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"degrees": ["BSc", "MSc"]}""");

        preprocess(context, "degrees", "degrees");

        assertEquals(List.of("BSc", "MSc"), attributeValues(context, "degrees"));
    }

    @Test
    public void mapsArrayOfObjectsToJsonElements() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"nationalities": [{"country": "DE"}, {"country": "CZ"}]}""");

        preprocess(context, "nationalities", "nationalities");

        assertEquals(List.of("{\"country\":\"DE\"}", "{\"country\":\"CZ\"}"),
                attributeValues(context, "nationalities"));
    }

    @Test
    public void mapsFannedOutMatchesToMultivaluedAttribute() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"degrees": [{"type": "BSc"}, {"type": "MSc"}]}""");

        preprocess(context, "degrees[].type", "degreeTypes");

        assertEquals(List.of("BSc", "MSc"), attributeValues(context, "degreeTypes"));
    }

    @Test
    public void mapsObjectClaimToJsonAttribute() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"address": {"street_address": "221B Baker Street", "locality": "London"}}""");

        preprocess(context, "address", "addressJson");

        assertEquals("{\"street_address\":\"221B Baker Street\",\"locality\":\"London\"}",
                context.getUserAttribute("addressJson"));
    }

    @Test
    public void missingClaimSetsNoAttribute() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"email": "alice@email.cz"}""");

        preprocess(context, "phone", "phone");

        assertNull(context.getUserAttribute("phone"));
    }

    @Test
    public void invalidClaimPathMapsNothing() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"degrees": ["BSc"]}""");

        preprocess(context, "degrees[x]", "notAnIndex");

        assertNull(context.getUserAttribute("notAnIndex"));
    }

    @Test
    public void mapsSpecialTargetsToUserProperties() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"sub": "alice", "email": "alice@email.cz", "given_name": "Alice", "family_name": "Wonder"}""");

        preprocess(context, "sub", "username");
        preprocess(context, "email", "email");
        preprocess(context, "given_name", "firstName");
        preprocess(context, "family_name", "lastName");

        assertEquals("alice", context.getModelUsername());
        assertEquals("alice@email.cz", context.getEmail());
        assertEquals("Alice", context.getFirstName());
        assertEquals("Wonder", context.getLastName());
    }

    @Test
    public void blankConfigurationMapsNothing() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"email": "alice@email.cz"}""");

        preprocess(context, "", "emailAttribute");
        preprocess(context, "email", "");

        assertNull(context.getUserAttribute("emailAttribute"));
        assertNull(context.getEmail());
    }

    @Test
    public void mapsClaimsRestoredFromSerializedContext() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"address": {"locality": "London"}}""");
        Object restored = JsonSerialization.readValue(
                JsonSerialization.writeValueAsString(
                        context.getContextData().get(OID4VPIdentityProvider.CREDENTIAL_CLAIMS)),
                Map.class);
        context.getContextData().put(OID4VPIdentityProvider.CREDENTIAL_CLAIMS, restored);

        preprocess(context, "address.locality", "locality");

        assertEquals("London", context.getUserAttribute("locality"));
    }

    @Test
    public void supportsAllSyncModes() {
        for (IdentityProviderSyncMode syncMode : IdentityProviderSyncMode.values()) {
            assertTrue(mapper.supportsSyncMode(syncMode));
        }
    }

    @Test
    public void isCompatibleWithTheOid4vpProvider() {
        assertEquals(List.of("oid4vp"), List.of(mapper.getCompatibleProviders()));
    }

    private void preprocess(BrokeredIdentityContext context, String claim, String attribute) {
        IdentityProviderMapperModel model = new IdentityProviderMapperModel();
        model.setName("test-mapper");
        model.setConfig(Map.of(
                OID4VPSdJwtUserAttributeMapper.CLAIM, claim,
                OID4VPSdJwtUserAttributeMapper.USER_ATTRIBUTE, attribute));
        mapper.preprocessFederatedIdentity(null, null, model, context);
    }

    private static BrokeredIdentityContext contextWithClaims(String claimsJson) throws Exception {
        JsonNode claims = JsonSerialization.readValue(claimsJson, JsonNode.class);
        OID4VPIdentityProviderConfig config = new OID4VPIdentityProviderConfig();
        config.setAlias("oid4vp");
        config.setEnabled(true);
        BrokeredIdentityContext context = new BrokeredIdentityContext("test-user", config);
        context.getContextData().put(OID4VPIdentityProvider.CREDENTIAL_CLAIMS, claims);
        return context;
    }

    @SuppressWarnings("unchecked")
    private static List<String> attributeValues(BrokeredIdentityContext context, String attribute) {
        return (List<String>) context.getContextData().get("user.attributes." + attribute);
    }
}
