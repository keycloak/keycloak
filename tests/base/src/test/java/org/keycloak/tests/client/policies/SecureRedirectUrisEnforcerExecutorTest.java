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
package org.keycloak.tests.client.policies;

import java.util.Collections;

import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.SecureRedirectUrisEnforcerExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.Test;

import static org.keycloak.tests.utils.ClientPoliciesUtil.createAnyClientConditionConfig;
import static org.keycloak.tests.utils.ClientPoliciesUtil.createSecureRedirectUrisEnforcerExecutorConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest
public class SecureRedirectUrisEnforcerExecutorTest extends AbstractClientPoliciesTest {

    private static final String HTTP_ATTACKER_URI  = "http://attacker.example.com/post-logout";
    private static final String HTTPS_SAFE_URI     = "https://app.example.com/logout";
    private static final String HTTPS_CALLBACK     = "https://app.example.com/callback";

    @InjectRealm
    protected ManagedRealm realm;

    @Test
    public void testCreateWithFlowsDisabledAndHttpPostLogoutUriIsRejected() throws Exception {
        setupSecureRedirectPolicy();

        ClientPolicyException cpe = assertThrows(ClientPolicyException.class, () ->
            createClientByAdmin(realm, generateSuffixedName("client-a"), OIDCLoginProtocol.LOGIN_PROTOCOL, rep -> {
                rep.setStandardFlowEnabled(Boolean.FALSE);
                rep.setImplicitFlowEnabled(Boolean.FALSE);
                rep.setRedirectUris(Collections.singletonList(HTTPS_CALLBACK));
                rep.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, HTTP_ATTACKER_URI);
            })
        );
        assertEquals("invalid_request", cpe.getMessage());
    }

    @Test
    public void testUpdateExplicitHttpPostLogoutUriWithFlowsDisabledIsRejected() throws Exception {
        setupSecureRedirectPolicy();

        String cId = createClientByAdmin(realm, generateSuffixedName("client-b"), OIDCLoginProtocol.LOGIN_PROTOCOL, rep -> {
            rep.setStandardFlowEnabled(Boolean.TRUE);
            rep.setRedirectUris(Collections.singletonList(HTTPS_CALLBACK));
            rep.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, HTTPS_SAFE_URI);
        });

        ClientPolicyException cpe = assertThrows(ClientPolicyException.class, () ->
            updateClientByAdmin(realm, cId, rep -> {
                rep.setStandardFlowEnabled(Boolean.FALSE);
                rep.setImplicitFlowEnabled(Boolean.FALSE);
                rep.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, HTTP_ATTACKER_URI);
            })
        );
        assertEquals("invalid_request", cpe.getMessage());
    }

    @Test
    public void testPartialUpdateSettingHttpPostLogoutUriWithFlowsDisabledIsRejected() throws Exception {
        setupSecureRedirectPolicy();

        String cId = createClientByAdmin(realm, generateSuffixedName("client-c"), OIDCLoginProtocol.LOGIN_PROTOCOL, rep -> {
            rep.setStandardFlowEnabled(Boolean.FALSE);
            rep.setImplicitFlowEnabled(Boolean.FALSE);
            rep.setRedirectUris(Collections.singletonList(HTTPS_CALLBACK));
        });

        ClientPolicyException cpe = assertThrows(ClientPolicyException.class, () ->
            updateClientByAdmin(realm, cId, rep ->
                rep.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, HTTP_ATTACKER_URI)
            )
        );
        assertEquals("invalid_request", cpe.getMessage());
    }

    @Test
    public void testPartialUpdateSettingHttpsPostLogoutUriWithFlowsDisabledIsAllowed() throws Exception {
        setupSecureRedirectPolicy();

        String cId = createClientByAdmin(realm, generateSuffixedName("client-c-pos"), OIDCLoginProtocol.LOGIN_PROTOCOL, rep -> {
            rep.setStandardFlowEnabled(Boolean.FALSE);
            rep.setImplicitFlowEnabled(Boolean.FALSE);
            rep.setRedirectUris(Collections.singletonList(HTTPS_CALLBACK));
        });

        updateClientByAdmin(realm, cId, rep ->
            rep.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, HTTPS_SAFE_URI)
        );
    }

    @Test
    public void testLegitimateHttpsPostLogoutUriNotBroken() throws Exception {
        setupSecureRedirectPolicy();

        String cId = createClientByAdmin(realm, generateSuffixedName("client-d"), OIDCLoginProtocol.LOGIN_PROTOCOL, rep -> {
            rep.setStandardFlowEnabled(Boolean.TRUE);
            rep.setRedirectUris(Collections.singletonList(HTTPS_CALLBACK));
            rep.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, HTTPS_SAFE_URI);
        });

        // Partial UPDATE omitting attributes — stored HTTPS URI must pass revalidation
        updateClientByAdmin(realm, cId, rep -> rep.setName("updated-name"));
    }

    // Ensures partial updates with a null rootUrl still allow valid absolute stored URIs.
    @Test
    public void testPartialUpdate_nullRootUrl_keepsStoredPostLogoutUris() throws Exception {
        setupSecureRedirectPolicy();

        String cId = createClientByAdmin(realm, generateSuffixedName("client-root-url"), OIDCLoginProtocol.LOGIN_PROTOCOL, rep -> {
            rep.setStandardFlowEnabled(Boolean.TRUE);
            rep.setRootUrl("https://app.example.com");
            rep.setRedirectUris(Collections.singletonList(HTTPS_CALLBACK));
            rep.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, HTTPS_SAFE_URI);
        });

        // Updating basic fields shouldn't affect post-logout URIs
        updateClientByAdmin(realm, cId, rep -> rep.setName("updated-root-url-client"));

        // Clearing rootUrl shouldn't break fallback validation for stored absolute URIs
        updateClientByAdmin(realm, cId, rep -> {
            rep.setRootUrl(null);
            rep.getAttributes().remove(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS);
        });
    }

    @Test
    public void testCreateWithNoPostLogoutUriSucceeds() throws Exception {
        setupSecureRedirectPolicy();

        createClientByAdmin(realm, generateSuffixedName("client-d2"), OIDCLoginProtocol.LOGIN_PROTOCOL, rep -> {
            rep.setStandardFlowEnabled(Boolean.TRUE);
            rep.setRedirectUris(Collections.singletonList(HTTPS_CALLBACK));
        });
    }

    private void setupSecureRedirectPolicy() throws Exception {
        setupPolicy(
            realm,
            SecureRedirectUrisEnforcerExecutorFactory.PROVIDER_ID,
            createSecureRedirectUrisEnforcerExecutorConfig(cfg -> {}),
            AnyClientConditionFactory.PROVIDER_ID,
            createAnyClientConditionConfig()
        );
    }
}
