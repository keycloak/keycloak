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

import java.util.List;
import java.util.Map;

import org.keycloak.OAuth2Constants;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.events.Errors;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextCondition;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextConditionFactory;
import org.keycloak.services.clientpolicy.executor.JWTAuthorizationGrantSettingsDisabledExecutorFactory;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakUrls;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:mahdi.a.alhakim@gmail.com">Mahdi Alhakim</a>
 */
@KeycloakIntegrationTest
public class JWTAuthorizationGrantSettingsDisabledExecutorTest extends AbstractClientPoliciesTest {

    private static final String IDP_ALIAS = "my-idp";
    private static final String AUDIENCE = "[{\"key\":\"my-idp\",\"value\":\"my-audience\"}]";
    private static final String OTHER_AUDIENCE = "[{\"key\":\"my-idp\",\"value\":\"other-audience\"}]";

    @InjectRealm
    protected ManagedRealm realm;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void testSettingsRejectedOnCreate() throws Exception {
        setupPolicy();

        // a client that does not carry the settings is unaffected
        createClientByAdmin(realm, generateSuffixedName("no-settings"), OIDCLoginProtocol.LOGIN_PROTOCOL, c -> {
        });

        ClientPolicyException cpe = Assertions.assertThrows(ClientPolicyException.class,
                () -> createClientByAdmin(realm, generateSuffixedName("with-idp"), OIDCLoginProtocol.LOGIN_PROTOCOL,
                        c -> c.getAttributes().put(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP, IDP_ALIAS)));
        assertNamesAttribute(cpe, OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP);

        cpe = Assertions.assertThrows(ClientPolicyException.class,
                () -> createClientByAdmin(realm, generateSuffixedName("with-audience"), OIDCLoginProtocol.LOGIN_PROTOCOL,
                        c -> c.getAttributes().put(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_AUDIENCE, AUDIENCE)));
        assertNamesAttribute(cpe, OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_AUDIENCE);
    }

    @Test
    public void testSettingsRejectedOnUpdate() throws Exception {
        setupPolicy();

        String cId = createClientByAdmin(realm, generateSuffixedName("update-target"), OIDCLoginProtocol.LOGIN_PROTOCOL, c -> {
        });

        ClientPolicyException cpe = Assertions.assertThrows(ClientPolicyException.class,
                () -> updateClientByAdmin(realm, cId,
                        c -> c.getAttributes().put(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP, IDP_ALIAS)));
        assertNamesAttribute(cpe, OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP);

        cpe = Assertions.assertThrows(ClientPolicyException.class,
                () -> updateClientByAdmin(realm, cId,
                        c -> c.getAttributes().put(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_AUDIENCE, AUDIENCE)));
        assertNamesAttribute(cpe, OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_AUDIENCE);
    }

    /**
     * The settings a client already has must survive the policy being enabled, and the rest of the
     * client must stay editable. An update sends the whole representation back, so the existing
     * value is repeated on every unrelated change and must not be treated as a new setting.
     */
    @Test
    public void testExistingSettingsPreservedAndClientStillEditable() throws Exception {
        String cId = createClientByAdmin(realm, generateSuffixedName("preexisting"), OIDCLoginProtocol.LOGIN_PROTOCOL, c -> {
            c.getAttributes().put(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP, IDP_ALIAS);
            c.getAttributes().put(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_AUDIENCE, AUDIENCE);
        });

        setupPolicy();

        // an unrelated change repeats both settings unchanged and must be allowed
        updateClientByAdmin(realm, cId, c -> c.setDescription("edited while the policy is active"));

        ClientRepresentation stored = realm.admin().clients().get(cId).toRepresentation();
        Assertions.assertEquals("edited while the policy is active", stored.getDescription());
        Assertions.assertEquals(IDP_ALIAS, stored.getAttributes().get(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP));
        Assertions.assertEquals(AUDIENCE, stored.getAttributes().get(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_AUDIENCE));

        // changing one of them is still rejected
        ClientPolicyException cpe = Assertions.assertThrows(ClientPolicyException.class,
                () -> updateClientByAdmin(realm, cId,
                        c -> c.getAttributes().put(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_AUDIENCE, OTHER_AUDIENCE)));
        assertNamesAttribute(cpe, OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_AUDIENCE);

        // and removing one of them is rejected too
        cpe = Assertions.assertThrows(ClientPolicyException.class,
                () -> updateClientByAdmin(realm, cId,
                        c -> c.getAttributes().put(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP, "")));
        assertNamesAttribute(cpe, OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP);
    }

    /**
     * Without the executor in a policy the behaviour is unchanged, which is what makes the
     * assertions above attributable to the executor rather than to something else.
     */
    @Test
    public void testSettingsAllowedWithoutThePolicy() throws Exception {
        String cId = createClientByAdmin(realm, generateSuffixedName("no-policy"), OIDCLoginProtocol.LOGIN_PROTOCOL,
                c -> c.getAttributes().put(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP, IDP_ALIAS));

        updateClientByAdmin(realm, cId,
                c -> c.getAttributes().put(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_AUDIENCE, AUDIENCE));

        ClientRepresentation stored = realm.admin().clients().get(cId).toRepresentation();
        Assertions.assertEquals(IDP_ALIAS, stored.getAttributes().get(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP));
        Assertions.assertEquals(AUDIENCE, stored.getAttributes().get(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_AUDIENCE));
    }

    /**
     * The same rule has to hold for dynamic client registration, which is the path where the client
     * itself authors the request rather than an administrator.
     */
    @Test
    public void testSettingsRejectedThroughDynamicClientRegistration() throws Exception {
        setupPolicy();

        ClientRegistration reg = ClientRegistration.create().url(keycloakUrls.getBase(), realm.getName()).build();
        reg.auth(Auth.token(newInitialAccessToken()));

        ClientRepresentation withSettings = new ClientRepresentation();
        withSettings.setClientId(generateSuffixedName("dcr-with-idp"));
        withSettings.setAttributes(Map.of(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP, IDP_ALIAS));
        assertRegistrationRefused(Assertions.assertThrows(ClientRegistrationException.class, () -> reg.create(withSettings)));

        // a registration that does not carry the settings still works, so the refusal above is the
        // executor rather than the registration being broken
        reg.auth(Auth.token(newInitialAccessToken()));
        ClientRepresentation clean = new ClientRepresentation();
        clean.setClientId(generateSuffixedName("dcr-clean"));
        ClientRepresentation created = reg.create(clean);
        realm.cleanup().add(r -> r.clients().delete(created.getId()));

        // and the client cannot add them afterwards with its own registration access token
        reg.auth(Auth.token(created.getRegistrationAccessToken()));
        ClientRepresentation update = new ClientRepresentation();
        update.setClientId(created.getClientId());
        update.setAttributes(Map.of(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_AUDIENCE, AUDIENCE));
        assertRegistrationRefused(Assertions.assertThrows(ClientRegistrationException.class, () -> reg.update(update)));
    }

    /**
     * The point of doing this with a client policy is that a deployment can decide who it applies
     * to. Scoped to the token based sources with the client-updater-context condition, a client is
     * refused and an administrator is not, which is the case for a deployment that trusts its
     * client administrators with these settings but not the clients themselves.
     */
    @Test
    public void testCanBeScopedToClientRequestsOnly() throws Exception {
        ClientUpdaterContextCondition.Configuration conditionConfig = new ClientUpdaterContextCondition.Configuration();
        conditionConfig.setUpdateClientSource(List.of(
                ClientUpdaterContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN,
                ClientUpdaterContextConditionFactory.BY_INITIAL_ACCESS_TOKEN,
                ClientUpdaterContextConditionFactory.BY_ANONYMOUS));
        setupPolicy(realm, JWTAuthorizationGrantSettingsDisabledExecutorFactory.PROVIDER_ID,
                new ClientPolicyExecutorConfigurationRepresentation(),
                ClientUpdaterContextConditionFactory.PROVIDER_ID, conditionConfig);

        // the administrator is outside the condition, so this is allowed
        String cId = createClientByAdmin(realm, generateSuffixedName("scoped-admin"), OIDCLoginProtocol.LOGIN_PROTOCOL,
                c -> c.getAttributes().put(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP, IDP_ALIAS));
        ClientRepresentation stored = realm.admin().clients().get(cId).toRepresentation();
        Assertions.assertEquals(IDP_ALIAS, stored.getAttributes().get(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP));

        // the client is inside it, so the same attribute is refused
        ClientRegistration reg = ClientRegistration.create().url(keycloakUrls.getBase(), realm.getName()).build();
        reg.auth(Auth.token(newInitialAccessToken()));
        ClientRepresentation byClient = new ClientRepresentation();
        byClient.setClientId(generateSuffixedName("scoped-client"));
        byClient.setAttributes(Map.of(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP, IDP_ALIAS));
        assertRegistrationRefused(Assertions.assertThrows(ClientRegistrationException.class, () -> reg.create(byClient)));
    }

    /**
     * Enabling the grant on an existing client is refused, because the allow lists an administrator
     * configured earlier are still there and the flag alone would make the grant work again.
     * Turning it off has to stay allowed, since a client drops the grant type that way.
     */
    @Test
    public void testEnablingTheGrantOnAnExistingClientIsRejected() throws Exception {
        String cId = createClientByAdmin(realm, generateSuffixedName("enable-target"), OIDCLoginProtocol.LOGIN_PROTOCOL, c -> {
            c.getAttributes().put(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP, IDP_ALIAS);
            c.getAttributes().put(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED, "false");
        });

        setupPolicy();

        ClientPolicyException cpe = Assertions.assertThrows(ClientPolicyException.class,
                () -> updateClientByAdmin(realm, cId,
                        c -> c.getAttributes().put(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED, "true")));
        Assertions.assertTrue(cpe.getErrorDetail().contains("enable the JWT authorization grant"), cpe.getErrorDetail());

        // turning it off is still permitted
        updateClientByAdmin(realm, cId,
                c -> c.getAttributes().put(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED, "false"));
    }

    /**
     * The enabled flag is written from the standard OpenID Connect {@code grant_types} field, so a
     * registration that asks for the grant has to keep working while the policy is active, and so
     * does the read modify write that an update of an OpenID Connect client goes through.
     */
    @Test
    public void testGrantTypeAcceptedThroughOIDCDynamicClientRegistration() throws Exception {
        setupPolicy();

        ClientRegistration reg = newClientRegistration();
        OIDCClientRepresentation created = registerWithTheGrantType(reg, generateSuffixedName("oidc-with-grant"));
        String clientId = created.getClientId();

        // the response advertises the grant, so an update sends it back
        Assertions.assertTrue(created.getGrantTypes().contains(OAuth2Constants.JWT_AUTHORIZATION_GRANT));
        Assertions.assertEquals("true", storedAttribute(clientId, OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED));

        // an unrelated edit repeats grant_types unchanged, which is not a change
        updateClientDynamically(reg, clientId, c -> c.setClientName("edited while the policy is active"));
        Assertions.assertEquals("true", storedAttribute(clientId, OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED));
    }

    /**
     * A client drops the grant by leaving it out of grant_types and that stays allowed, but it
     * cannot put it back once the grant is off. The allow lists an administrator configured are
     * untouched throughout, since an OpenID Connect request cannot carry them.
     */
    @Test
    public void testClientCanDropTheGrantTypeButNotAddItBack() throws Exception {
        ClientRegistration reg = newClientRegistration();
        String clientId = registerWithTheGrantType(reg, generateSuffixedName("oidc-drop-grant")).getClientId();

        // the administrator adds the allow lists the grant needs
        updateClientByAdmin(realm, findByClientIdByAdmin(realm, clientId).getId(), c -> {
            c.getAttributes().put(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP, IDP_ALIAS);
            c.getAttributes().put(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_AUDIENCE, AUDIENCE);
        });

        setupPolicy();

        // the client leaves the grant out of its own registration
        updateClientDynamically(reg, clientId, c -> c.setGrantTypes(List.of(OAuth2Constants.AUTHORIZATION_CODE)));
        Assertions.assertEquals("false", storedAttribute(clientId, OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED));

        // and cannot put it back
        assertRegistrationRefused(Assertions.assertThrows(ClientRegistrationException.class,
                () -> updateClientDynamically(reg, clientId, c -> c.setGrantTypes(
                        List.of(OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.JWT_AUTHORIZATION_GRANT)))));

        ClientRepresentation stored = findByClientIdByAdmin(realm, clientId);
        Assertions.assertEquals("false", stored.getAttributes().get(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED));
        Assertions.assertEquals(IDP_ALIAS, stored.getAttributes().get(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP));
        Assertions.assertEquals(AUDIENCE, stored.getAttributes().get(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_AUDIENCE));
    }

    private void assertNamesAttribute(ClientPolicyException cpe, String attribute) {
        Assertions.assertTrue(cpe.getErrorDetail().contains(attribute),
                "the refusal should name the attribute that failed, was: " + cpe.getErrorDetail());
    }

    private void assertRegistrationRefused(ClientRegistrationException cre) {
        HttpErrorException cause = (HttpErrorException) cre.getCause();
        Assertions.assertEquals(403, cause.getStatusLine().getStatusCode());
        // the refusal is the client policy, not the request being unauthorized
        Assertions.assertEquals(Errors.INVALID_REGISTRATION, cause.toErrorRepresentation().getErrorDescription());
    }

    private ClientRegistration newClientRegistration() {
        ClientRegistration reg = ClientRegistration.create().url(keycloakUrls.getBase(), realm.getName()).build();
        reg.auth(Auth.token(newInitialAccessToken()));
        return reg;
    }

    private OIDCClientRepresentation registerWithTheGrantType(ClientRegistration reg, String clientName) throws Exception {
        OIDCClientRepresentation request = new OIDCClientRepresentation();
        request.setClientName(clientName);
        request.setClientUri(realm.getBaseUrl());
        request.setRedirectUris(List.of(realm.getBaseUrl() + "/app/auth"));
        request.setGrantTypes(List.of(OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.JWT_AUTHORIZATION_GRANT));

        OIDCClientRepresentation created = reg.oidc().create(request);
        reg.auth(Auth.token(created));

        String cId = findByClientIdByAdmin(realm, created.getClientId()).getId();
        realm.cleanup().add(r -> r.clients().delete(cId));
        return created;
    }

    private String storedAttribute(String clientId, String attribute) throws Exception {
        return findByClientIdByAdmin(realm, clientId).getAttributes().get(attribute);
    }

    private String newInitialAccessToken() {
        ClientInitialAccessCreatePresentation rep = new ClientInitialAccessCreatePresentation();
        rep.setCount(1);
        rep.setExpiration(10000);
        return realm.admin().clientInitialAccess().create(rep).getToken();
    }

    private void setupPolicy() throws Exception {
        setupPolicy(realm, JWTAuthorizationGrantSettingsDisabledExecutorFactory.PROVIDER_ID,
                new ClientPolicyExecutorConfigurationRepresentation(),
                AnyClientConditionFactory.PROVIDER_ID, new ClientPolicyConditionConfigurationRepresentation());
    }
}
