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
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OID4VPSdJwtUserSessionAttributeMapperTest {

    private final OID4VPSdJwtUserSessionAttributeMapper mapper = new OID4VPSdJwtUserSessionAttributeMapper();

    @Test
    public void importsStringClaimAsSessionNote() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"vct": "https://credentials.example.com/identity"}""");

        importNewUser(context, "vct", "credential_vct");

        assertEquals("https://credentials.example.com/identity", sessionNote(context, "credential_vct"));
    }

    @Test
    public void importsNestedAndArrayClaims() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"address": {"locality": "London"}, "nationalities": ["DE", "CZ"]}""");

        importNewUser(context, "address.locality", "locality");
        importNewUser(context, "nationalities", "nationalities");

        assertEquals("London", sessionNote(context, "locality"));
        assertEquals("DE,CZ", sessionNote(context, "nationalities"));
    }

    @Test
    public void importsObjectClaimAsJson() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"address": {"locality": "London"}}""");

        importNewUser(context, "address", "addressJson");

        assertEquals("{\"locality\":\"London\"}", sessionNote(context, "addressJson"));
    }

    @Test
    public void missingClaimSetsNoNote() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"vct": "identity"}""");

        importNewUser(context, "phone", "phone");

        assertNull(sessionNote(context, "phone"));
    }

    @Test
    public void blankConfigurationSetsNoNote() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"vct": "identity"}""");

        importNewUser(context, "", "note");
        importNewUser(context, "vct", "");

        assertNull(sessionNote(context, "note"));
        assertNull(sessionNote(context, ""));
    }

    @Test
    public void updateBrokeredUserAlsoSetsTheNote() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"vct": "identity"}""");

        mapper.updateBrokeredUser(null, null, null, model("vct", "credential_vct"), context);

        assertEquals("identity", sessionNote(context, "credential_vct"));
    }

    // The note is per login state, so it must also be set on the preprocess path that runs for
    // existing users whose effective sync mode skips updateBrokeredUser.
    @Test
    public void preprocessFederatedIdentityAlsoSetsTheNote() throws Exception {
        BrokeredIdentityContext context = contextWithClaims("""
                {"vct": "identity"}""");

        mapper.preprocessFederatedIdentity(null, null, model("vct", "credential_vct"), context);

        assertEquals("identity", sessionNote(context, "credential_vct"));
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

    private void importNewUser(BrokeredIdentityContext context, String claimPath, String attribute) {
        mapper.importNewUser(null, null, null, model(claimPath, attribute), context);
    }

    private static IdentityProviderMapperModel model(String claimPath, String attribute) {
        IdentityProviderMapperModel model = new IdentityProviderMapperModel();
        model.setName("test-mapper");
        model.setConfig(Map.of(
                OID4VPSdJwtUserSessionAttributeMapper.CLAIM, claimPath,
                OID4VPSdJwtUserSessionAttributeMapper.ATTRIBUTE, attribute));
        return model;
    }

    // Without an authentication session the context stores session notes in its context data.
    @SuppressWarnings("unchecked")
    private static String sessionNote(BrokeredIdentityContext context, String name) {
        Map<String, String> notes =
                (Map<String, String>) context.getContextData().get(Constants.MAPPER_SESSION_NOTES);
        return notes == null ? null : notes.get(name);
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
}
