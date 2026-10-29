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

package org.keycloak.tests.oid4vc;

import java.util.List;

import org.keycloak.admin.client.resource.ComponentsResource;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class CredentialSigningAlgorithmResolverTest extends OID4VCIssuerTestBase {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @TestSetup
    public void configureTestRealm() {
        super.configureTestRealm();
        ComponentsResource components = testRealm.admin().components();
        components.add(getRsaKeyProvider(getRsaKey_Default())).close();
    }

    @Test
    public void metadataSigningAlgorithmsFallBackToGlobalListWithoutConfiguredKey() {
        runOnServer.run(session -> {
            KeyWrapper key = getKeyFromSession(session);
            List<String> globalSupportedSigningAlgorithms = List.of("PS256", "ES256", key.getAlgorithm());

            CredentialScopeModel credentialScope = jwtCredentialScope(session);
            credentialScope.setSigningKeyId(null);
            credentialScope.setSigningAlg(null);

            SupportedCredentialConfiguration config = SupportedCredentialConfiguration.parse(
                    session, credentialScope, globalSupportedSigningAlgorithms);

            assertEquals(globalSupportedSigningAlgorithms, config.getCredentialSigningAlgValuesSupported());
        });
    }

    @Test
    public void metadataSigningAlgorithmFollowsConfiguredSigningKey() {
        runOnServer.run(session -> {
            KeyWrapper key = getKeyFromSession(session);
            List<String> globalSupportedSigningAlgorithms = List.of("PS256", "ES256", key.getAlgorithm());

            CredentialScopeModel credentialScope = jwtCredentialScope(session);
            credentialScope.setSigningKeyId(key.getKid());
            credentialScope.setSigningAlg(null);

            SupportedCredentialConfiguration config = SupportedCredentialConfiguration.parse(
                    session, credentialScope, globalSupportedSigningAlgorithms);

            assertEquals(List.of(key.getAlgorithm()), config.getCredentialSigningAlgValuesSupported());
        });
    }

    private static CredentialScopeModel jwtCredentialScope(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        return new CredentialScopeModel(
                realm.getClientScopesStream()
                        .filter(cs -> jwtTypeCredentialScopeName.equals(cs.getName()))
                        .findFirst()
                        .orElseThrow());
    }
}
