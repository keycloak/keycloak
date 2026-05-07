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
package org.keycloak.tests.broker;

import java.security.PublicKey;

import org.keycloak.common.VerificationException;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.saml.SamlProtocolUtils;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.rotation.HardcodedKeyLocator;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;
import org.keycloak.testsuite.util.oauth.PlainStringResponse;

import org.junit.jupiter.api.Assertions;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest
public class SamlIdentityProviderStoreTokenV1Test implements InterfaceIdentityProviderStoreTokenV1Test, InterfaceSamlIdentityProviderStoreTokenTest {

    @InjectRealm(config = IdpRealmConfig.class)
    protected ManagedRealm realm;

    @InjectRealm(ref = "external-realm", config = ExternalRealmConfig.class)
    ManagedRealm externalRealm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Override
    public ManagedRealm getRealm() {
        return realm;
    }

    @Override
    public ManagedRealm getExternalRealm() {
        return externalRealm;
    }

    @Override
    public OAuthClient getOAuthClient() {
        return oauth;
    }

    @Override
    public LoginPage getLoginPage() {
        return loginPage;
    }

    @Override
    public RunOnServerClient getRunOnServer() {
        return runOnServer;
    }

    @Override
    public AbstractHttpResponse doFetchExternalIdpToken(String token) {
        return getOAuthClient().doFetchExternalIdpTokenString(IDP_ALIAS, token);
    }

    @Override
    public void checkSuccessfulTokenResponse(AbstractHttpResponse response) {
        Assertions.assertInstanceOf(PlainStringResponse.class, response);
        PlainStringResponse plainStringResponse = (PlainStringResponse) response;
        SAMLDocumentHolder holder = SAMLRequestParser.parseResponsePostBinding(plainStringResponse.getResponse());
        Assertions.assertNotNull(holder);

        KeysMetadataRepresentation keysMetadata = getExternalRealm().admin().keys().getKeyMetadata();
        String kid = keysMetadata.getActive().get("RS256");
        KeysMetadataRepresentation.KeyMetadataRepresentation keyMetadata = keysMetadata.getKeys().stream()
                .filter(k -> kid.equals(k.getKid())).findAny().orElse(null);
        PublicKey realmPubKey = KeycloakModelUtils.getPublicKey(keyMetadata.getPublicKey());

        try {
            SamlProtocolUtils.verifyDocumentSignature(holder.getSamlDocument(), new HardcodedKeyLocator(realmPubKey));
        } catch (VerificationException e) {
            throw new RuntimeException(e);
        }
    }
}
