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
package org.keycloak.services.clientpolicy.executor;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Verifies the merge behind {@code SecureClientUrisExecutor#effectiveClientRepresentation}. The merge has to
 * mirror the persistence semantics of {@code RepresentationToModel#updateClientProperties}: an attribute set
 * to null (or to an empty value) removes the stored key during the update - a remediation deleting a stored
 * URI must not keep the stale URI in the effective state - and web origins omitted from an update of a
 * client without stored web origins are derived from the effective redirect URIs
 * (see {@code RepresentationToModel#defaultWebOrigins}).
 *
 * The explicit null case is covered on this level because the admin client does not serialize null attribute
 * values (JsonInclude.NON_NULL), so it can only be produced by a raw HTTP update; the end-to-end deletion
 * semantics are covered by the empty-value test in the testsuite.
 */
public class SecureClientUrisExecutorTest {

    private static final String STORED_JWKS_URL = "http://stored.example.com/jwks";

    @Test
    public void updateOmittingFieldsKeepsStoredValues() {
        ClientRepresentation proposed = new ClientRepresentation();
        proposed.setDescription("updated description");

        ClientRepresentation effective = SecureClientUrisExecutor.effectiveClientRepresentation(proposed,
                storedClient(Map.of(OIDCConfigAttributes.JWKS_URL, STORED_JWKS_URL),
                        Set.of("https://stored.example.com/callback"),
                        Set.of("https://stored.example.com")));

        assertEquals("https://stored.example.com", effective.getRootUrl());
        assertEquals("http://stored.example.com/admin", effective.getAdminUrl());
        assertEquals("https://stored.example.com/base", effective.getBaseUrl());
        assertEquals(List.of("https://stored.example.com/callback"), effective.getRedirectUris());
        assertEquals(List.of("https://stored.example.com"), effective.getWebOrigins());
        assertEquals(STORED_JWKS_URL, effective.getAttributes().get(OIDCConfigAttributes.JWKS_URL));
    }

    @Test
    public void updateSettingAttributeToNullRemovesStoredKey() {
        ClientRepresentation proposed = new ClientRepresentation();
        Map<String, String> attributes = new HashMap<>();
        attributes.put(OIDCConfigAttributes.JWKS_URL, null);
        proposed.setAttributes(attributes);

        ClientRepresentation effective = SecureClientUrisExecutor.effectiveClientRepresentation(proposed,
                storedClient(Map.of(OIDCConfigAttributes.JWKS_URL, STORED_JWKS_URL),
                        Collections.emptySet(), Collections.emptySet()));

        assertNull(effective.getAttributes().get(OIDCConfigAttributes.JWKS_URL));
    }

    @Test
    public void updateSettingAttributeToEmptyValueRemovesStoredKey() {
        assertDeletionRemovesStoredKey("");
    }

    @Test
    public void updateSettingAttributeToBlankValueRemovesStoredKey() {
        assertDeletionRemovesStoredKey(" ");
    }

    @Test
    public void updateReplacingAttributeKeepsNewValue() {
        ClientRepresentation proposed = new ClientRepresentation();
        proposed.setAttributes(Map.of(OIDCConfigAttributes.JWKS_URL, "https://secure.example.com/jwks"));

        ClientRepresentation effective = SecureClientUrisExecutor.effectiveClientRepresentation(proposed,
                storedClient(Map.of(OIDCConfigAttributes.JWKS_URL, STORED_JWKS_URL),
                        Collections.emptySet(), Collections.emptySet()));

        assertEquals("https://secure.example.com/jwks", effective.getAttributes().get(OIDCConfigAttributes.JWKS_URL));
    }

    @Test
    public void updateOmittingWebOriginsDerivesThemFromEffectiveRedirectUris() {
        ClientRepresentation proposed = new ClientRepresentation();
        proposed.setRedirectUris(List.of("https://trusted.example.com/callback", "custom://app"));

        ClientRepresentation effective = SecureClientUrisExecutor.effectiveClientRepresentation(proposed,
                storedClient(Collections.emptyMap(), Set.of("https://stored.example.com/callback"),
                        Collections.emptySet()));

        assertEquals(List.of("https://trusted.example.com"), effective.getWebOrigins());
    }

    @Test
    public void updateOmittingWebOriginsKeepsNonEmptyStoredOrigins() {
        ClientRepresentation proposed = new ClientRepresentation();
        proposed.setRedirectUris(List.of("https://trusted.example.com/callback"));

        ClientRepresentation effective = SecureClientUrisExecutor.effectiveClientRepresentation(proposed,
                storedClient(Collections.emptyMap(), Set.of("https://stored.example.com/callback"),
                        Set.of("https://stored.example.com")));

        assertEquals(List.of("https://stored.example.com"), effective.getWebOrigins());
    }

    @Test
    public void updateWithEmptyWebOriginsListClearsThem() {
        ClientRepresentation proposed = new ClientRepresentation();
        proposed.setWebOrigins(Collections.emptyList());

        ClientRepresentation effective = SecureClientUrisExecutor.effectiveClientRepresentation(proposed,
                storedClient(Collections.emptyMap(), Set.of("https://stored.example.com/callback"),
                        Set.of("https://stored.example.com")));

        assertEquals(Collections.emptyList(), effective.getWebOrigins());
    }

    private void assertDeletionRemovesStoredKey(String deletionValue) {
        ClientRepresentation proposed = new ClientRepresentation();
        proposed.setAttributes(Map.of(OIDCConfigAttributes.JWKS_URL, deletionValue));

        ClientRepresentation effective = SecureClientUrisExecutor.effectiveClientRepresentation(proposed,
                storedClient(Map.of(OIDCConfigAttributes.JWKS_URL, STORED_JWKS_URL),
                        Collections.emptySet(), Collections.emptySet()));

        assertNull(effective.getAttributes().get(OIDCConfigAttributes.JWKS_URL));
    }

    /**
     * A stored client that only answers the accessors the merge uses, see
     * {@code SecureClientUrisExecutor#effectiveClientRepresentation}. Any other method of the interface is
     * not invoked by the merge and defaults to null.
     */
    private static ClientModel storedClient(Map<String, String> attributes, Set<String> redirectUris, Set<String> webOrigins) {
        return (ClientModel) Proxy.newProxyInstance(SecureClientUrisExecutorTest.class.getClassLoader(),
                new Class<?>[] { ClientModel.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getAttributes": return attributes;
                        case "getRedirectUris": return redirectUris;
                        case "getWebOrigins": return webOrigins;
                        case "getRootUrl": return "https://stored.example.com";
                        case "getManagementUrl": return "http://stored.example.com/admin";
                        case "getBaseUrl": return "https://stored.example.com/base";
                        case "equals": return proxy == args[0];
                        case "hashCode": return System.identityHashCode(proxy);
                        case "toString": return "storedClient";
                        default: return null;
                    }
                });
    }
}
