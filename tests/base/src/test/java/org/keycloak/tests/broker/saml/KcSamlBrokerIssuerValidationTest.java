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
package org.keycloak.tests.broker.saml;

import java.util.List;
import java.util.function.Consumer;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.broker.AbstractKcSamlBrokerTest;
import org.keycloak.tests.saml.SamlBrowserlessBrokerClient;
import org.keycloak.tests.saml.SamlBrowserlessBrokerClient.IdentityProviderResponse;
import org.keycloak.tests.saml.SamlResponseDocument;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.IDP_ENTITY_ID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the SAML broker endpoint rejects assertions and responses whose issuer does not match the
 * configured IdP entity ID (CVE-2026-93998).
 *
 * <p>When {@code idpEntityId} is set on the identity provider, the broker validates both the response-level
 * and assertion-level {@code <saml:Issuer>} against it. An attacker controlling a sibling issuer that shares
 * the same signing key must not be able to forge assertions selecting arbitrary principals.
 */
@KeycloakIntegrationTest
public class KcSamlBrokerIssuerValidationTest extends AbstractKcSamlBrokerTest {

    private static final String ATTACKER_ISSUER = "https://attacker-tenant.idp.example";
    private static final String VICTIM = "victim";

    private String victimId;

    @BeforeEach
    void createVictimUser() {
        try (Response response = getConsumerRealm().admin().users().create(UserBuilder.create(VICTIM)
                .enabled(true)
                .email("victim@localhost.com")
                .emailVerified(true)
                .name("Victim", "User")
                .federatedLink(getIdpAlias(), VICTIM, VICTIM)
                .build())) {
            victimId = ApiUtil.getCreatedId(response);
        }
    }

    // ---- login issuer tests ----

    @Test
    public void testCorrectIssuerIsAccepted() {
        configureIdpEntityId();
        assertLogin(doc -> {}, true);
    }

    @Test
    public void testWrongAssertionIssuerIsRejected() {
        configureIdpEntityId();
        assertLogin(doc -> doc.setAssertionIssuer(ATTACKER_ISSUER), false);
    }

    @Test
    public void testWrongResponseIssuerIsRejected() {
        configureIdpEntityId();
        assertLogin(doc -> doc.setResponseIssuer(ATTACKER_ISSUER), false);
    }

    @Test
    public void testWrongBothIssuersRejected() {
        configureIdpEntityId();
        assertLogin(doc -> {
            doc.setResponseIssuer(ATTACKER_ISSUER);
            doc.setAssertionIssuer(ATTACKER_ISSUER);
        }, false);
    }

    @Test
    public void testNullAssertionIssuerIsRejected() {
        configureIdpEntityId();
        assertLogin(SamlResponseDocument::removeAssertionIssuer, false);
    }

    @Test
    public void testNullResponseIssuerWithCorrectAssertionIssuerIsAccepted() {
        configureIdpEntityId();
        assertLogin(SamlResponseDocument::removeResponseIssuer, true);
    }

    @Test
    public void testWrongAssertionIssuerWithNullResponseIssuerIsRejected() {
        configureIdpEntityId();
        assertLogin(doc -> {
            doc.removeResponseIssuer();
            doc.setAssertionIssuer(ATTACKER_ISSUER);
        }, false);
    }

    // ---- helpers ----

    private void assertLogin(Consumer<SamlResponseDocument> mutation, boolean shouldSucceed) {
        try (SamlBrowserlessBrokerClient broker = newBrokerClient()) {
            IdentityProviderResponse response = broker.login(getUserLogin(), getUserPassword());
            response.onResponse(doc -> {
                doc.setNameId(VICTIM);
                mutation.accept(doc);
            });
            int status = response.submit();

            if (shouldSucceed) {
                assertEquals(Response.Status.OK.getStatusCode(), status,
                        "Login with correct issuer should succeed");
                assertEquals(1, victimSessions().size(),
                        "A successful login should create a session for the victim");
            } else {
                // The broker returns BAD_REQUEST when issuer validation fails
                assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), status,
                        "Login with wrong issuer should be rejected");
                assertTrue(victimSessions().isEmpty(),
                        "A rejected login must not create a session for the victim");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void configureIdpEntityId() {
        IdentityProviderResource idpResource = getConsumerRealm().admin().identityProviders().get(getIdpAlias());
        IdentityProviderRepresentation idp = idpResource.toRepresentation();
        idp.getConfig().put(IDP_ENTITY_ID, getProviderRealm().getBaseUrl());
        idpResource.update(idp);
    }

    private SamlBrowserlessBrokerClient newBrokerClient() {
        return new SamlBrowserlessBrokerClient(getConsumerRealm(), SAML_CLIENT_ID_SALES_POST, IDP_SAML_ALIAS);
    }

    private List<UserSessionRepresentation> victimSessions() {
        return getConsumerRealm().admin().users().get(victimId).getUserSessions();
    }
}
