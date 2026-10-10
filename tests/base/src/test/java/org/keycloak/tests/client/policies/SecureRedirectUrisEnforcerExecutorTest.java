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
import java.util.List;

import org.keycloak.OAuthErrorException;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.SecureRedirectUrisEnforcerExecutor;
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

    private static final String HTTP_ATTACKER_URI   = "http://attacker.example.com/post-logout";
    private static final String HTTPS_SAFE_URI      = "https://app.example.com/logout";
    private static final String HTTPS_CALLBACK      = "https://app.example.com/callback";
    private static final String HTTP_INSECURE_ROOT  = "http://insecure.example.com";
    private static final String HTTPS_SECURE_ROOT   = "https://secure.example.com";
    private static final String RELATIVE_POST_LOGOUT = "/logout";

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

        // A full-representation update that does not touch the post-logout uris must not be rejected by the always-on revalidation.
        updateClientByAdmin(realm, cId, rep -> rep.setName("updated-name"));
    }

    // An absolute stored post-logout URI stays valid across ordinary updates.
    @Test
    public void testUpdateDoesNotDisturbValidAbsolutePostLogoutUri() throws Exception {
        setupSecureRedirectPolicy();

        String cId = createClientByAdmin(realm, generateSuffixedName("client-root-url"), OIDCLoginProtocol.LOGIN_PROTOCOL, rep -> {
            rep.setStandardFlowEnabled(Boolean.TRUE);
            rep.setRootUrl("https://app.example.com");
            rep.setRedirectUris(Collections.singletonList(HTTPS_CALLBACK));
            rep.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, HTTPS_SAFE_URI);
        });

        // Updating basic fields shouldn't affect post-logout URIs
        updateClientByAdmin(realm, cId, rep -> rep.setName("updated-root-url-client"));
        updateClientByAdmin(realm, cId, rep -> rep.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, ""));
    }

    @Test
    public void testPartialUpdateResolvesRelativeUriAgainstStoredInsecureRootUrl() throws Exception {
        // Seed a relative post-logout uri under an insecure root before the policy is in place.
        String cId = createClientByAdmin(realm, generateSuffixedName("client-stored-root"), OIDCLoginProtocol.LOGIN_PROTOCOL, rep -> {
            rep.setStandardFlowEnabled(Boolean.TRUE);
            rep.setRootUrl(HTTP_INSECURE_ROOT);
            rep.setRedirectUris(Collections.singletonList(HTTPS_CALLBACK));
            rep.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, RELATIVE_POST_LOGOUT);
        });

        setupSecureRedirectPolicy();

        // Neither rootUrl nor attributes are supplied, so both come from the stored client.
        ClientRepresentation partial = new ClientRepresentation();
        partial.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        partial.setStandardFlowEnabled(Boolean.TRUE);
        partial.setRedirectUris(List.of(HTTPS_CALLBACK));

        ClientPolicyException ex = assertThrows(ClientPolicyException.class, () ->
            updateClientByAdminPartial(realm, cId, partial)
        );
        assertEquals(OAuthErrorException.INVALID_REQUEST, ex.getError());

        assertEquals(SecureRedirectUrisEnforcerExecutor.ERR_NORMALURI, ex.getErrorDetail());
    }

    /** A rootUrl supplied by the update takes precedence over the stored one. */
    @Test
    public void testPartialUpdateProposedRootUrlOverridesStoredInsecureRootUrl() throws Exception {
        String cId = createClientByAdmin(realm, generateSuffixedName("client-new-root"), OIDCLoginProtocol.LOGIN_PROTOCOL, rep -> {
            rep.setStandardFlowEnabled(Boolean.TRUE);
            rep.setRootUrl(HTTP_INSECURE_ROOT);
            rep.setRedirectUris(Collections.singletonList(HTTPS_CALLBACK));
            rep.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, RELATIVE_POST_LOGOUT);
        });

        setupSecureRedirectPolicy();

        // The update moves the client to a secure root, so the same relative uri is now safe.
        ClientRepresentation partial = new ClientRepresentation();
        partial.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        partial.setStandardFlowEnabled(Boolean.TRUE);
        partial.setRootUrl(HTTPS_SECURE_ROOT);
        partial.setRedirectUris(List.of(HTTPS_CALLBACK));

        updateClientByAdminPartial(realm, cId, partial);
    }

    @Test
    public void testCreateWithNoPostLogoutUriSucceeds() throws Exception {
        setupSecureRedirectPolicy();

        createClientByAdmin(realm, generateSuffixedName("client-d2"), OIDCLoginProtocol.LOGIN_PROTOCOL, rep -> {
            rep.setStandardFlowEnabled(Boolean.TRUE);
            rep.setRedirectUris(Collections.singletonList(HTTPS_CALLBACK));
            rep.getAttributes().remove(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS);
        });
    }

    /**
     * Two-step partial-update bypass (issue #50982).
     *
     * Step 1: seed an HTTP post-logout URI into the model before the policy is active.
     * Step 2: send a truly partial update — no attributes map, but safe redirectUris and
     *         standard flow re-enabled. Without the fix, OIDCAdvancedConfigWrapper falls
     *         back to clientRep.getRedirectUris() and returns the safe value, masking the
     *         stored HTTP URI. With the fix the executor reads the stored model and rejects.
     */
    @Test
    public void testPartialUpdateOmittingAttributesMasksStoredHttpPostLogoutUri() throws Exception {
        // Seed a dangerous URI before the policy is in place.
        String cId = createClientByAdmin(realm, generateSuffixedName("client-bypass"), OIDCLoginProtocol.LOGIN_PROTOCOL, rep -> {
            rep.setStandardFlowEnabled(Boolean.FALSE);
            rep.setImplicitFlowEnabled(Boolean.FALSE);
            rep.setRedirectUris(Collections.singletonList(HTTPS_CALLBACK));
            rep.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, HTTP_ATTACKER_URI);
        });

        // Now activate the policy.
        setupSecureRedirectPolicy();

        // Partial update: no attributes map at all (post.logout.redirect.uris absent),
        // safe redirectUris provided, standard flow re-enabled.
        ClientRepresentation partial = new ClientRepresentation();
        partial.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        partial.setStandardFlowEnabled(Boolean.TRUE);
        partial.setImplicitFlowEnabled(Boolean.FALSE);
        partial.setRedirectUris(List.of(HTTPS_CALLBACK));
        // attributes intentionally null — executor must fall back to stored HTTP post-logout URI

        ClientPolicyException ex = assertThrows(ClientPolicyException.class, () ->
            updateClientByAdminPartial(realm, cId, partial)
        );
        assertEquals(OAuthErrorException.INVALID_REQUEST, ex.getError());
    }

    /** "+" with an HTTP redirect URI must be rejected even when flows are disabled. */
    @Test
    public void testPlusPlaceholderExpandingToHttpUriIsRejected() throws Exception {
        setupSecureRedirectPolicy();

        ClientPolicyException ex = assertThrows(ClientPolicyException.class, () ->
            createClientByAdmin(realm, generateSuffixedName("client-plus-http"), OIDCLoginProtocol.LOGIN_PROTOCOL, rep -> {
                rep.setStandardFlowEnabled(Boolean.FALSE);
                rep.setImplicitFlowEnabled(Boolean.FALSE);
                rep.setRedirectUris(Collections.singletonList("http://oauth.redirect/some"));
                OIDCAdvancedConfigWrapper.fromClientRepresentation(rep).setPostLogoutRedirectUris(List.of("+"));
            })
        );
        assertEquals(OAuthErrorException.INVALID_REQUEST, ex.getError());
    }

    /** "+" with an HTTPS redirect URI is allowed even when flows are disabled. */
    @Test
    public void testPlusPlaceholderExpandingToHttpsUriIsAllowed() throws Exception {
        setupSecureRedirectPolicy();

        createClientByAdmin(realm, generateSuffixedName("client-plus-https"), OIDCLoginProtocol.LOGIN_PROTOCOL, rep -> {
            rep.setStandardFlowEnabled(Boolean.FALSE);
            rep.setImplicitFlowEnabled(Boolean.FALSE);
            rep.setRedirectUris(Collections.singletonList(HTTPS_CALLBACK));
            OIDCAdvancedConfigWrapper.fromClientRepresentation(rep).setPostLogoutRedirectUris(List.of("+"));
        });
    }

    @Test
    public void testCreateWithPlusPlaceholderAndNoRedirectUrisSucceeds() throws Exception {
        setupSecureRedirectPolicy();

        // "+" with no redirect uris to inherit expands to nothing — there is nothing to validate, and this must not blow up on a null list
        createClientByAdmin(realm, generateSuffixedName("client-plus-empty"), OIDCLoginProtocol.LOGIN_PROTOCOL, rep -> {
            rep.setStandardFlowEnabled(Boolean.FALSE);
            rep.setImplicitFlowEnabled(Boolean.FALSE);
            rep.setRedirectUris(null);
            rep.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+");
        });
    }

    @Test
    public void testBlankPostLogoutAttributeInheritingHttpUriIsRejected() throws Exception {
        setupSecureRedirectPolicy();

        ClientPolicyException ex = assertThrows(ClientPolicyException.class, () ->
            createClientByAdmin(realm, generateSuffixedName("client-blank-http"), OIDCLoginProtocol.LOGIN_PROTOCOL, rep -> {
                rep.setStandardFlowEnabled(Boolean.FALSE);
                rep.setImplicitFlowEnabled(Boolean.FALSE);
                rep.setRedirectUris(Collections.singletonList("http://oauth.redirect/some"));
                rep.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "");
            })
        );
        assertEquals(OAuthErrorException.INVALID_REQUEST, ex.getError());
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
