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

package org.keycloak.tests.oid4vc.presentation;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import org.keycloak.broker.oid4vp.OID4VPIdentityProviderConfig;
import org.keycloak.broker.oid4vp.OID4VPIdentityProviderFactory;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.admin.client.resource.IdentityProviderResource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The wallet scheme is rendered as the href of the wallet button on the login page, so the admin
 * API must reject schemes a browser executes or resolves locally (javascript, data, ...) when the
 * identity provider is created or updated, while still allowing custom wallet schemes.
 */
@KeycloakIntegrationTest(config = PresentationServerConfig.class)
public class OID4VPWalletSchemeValidationTest {

    private static final String ALIAS = "oid4vp-wallet-scheme";

    @InjectRealm
    ManagedRealm realm;

    @Test
    public void cannotCreateIdentityProviderWithJavascriptWalletScheme() {
        // The exact value from the issue report.
        try (Response response = realm.admin().identityProviders().create(idp(ALIAS, "javascript:alert(document.domain)//"))) {
            Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            Assertions.assertTrue(response.readEntity(String.class).contains("Wallet scheme"),
                    "The rejection must name the wallet scheme, not an unrelated validation");
        }
    }

    @Test
    public void cannotCreateIdentityProviderWithDangerousSchemeVariants() {
        for (String walletScheme : new String[] {"javascript", "JavaScript://", "data", "vbscript://", "blob", "about"}) {
            try (Response response = realm.admin().identityProviders().create(idp(ALIAS, walletScheme))) {
                Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(),
                        "walletScheme '" + walletScheme + "' must be rejected");
            }
        }
    }

    @Test
    public void cannotCreateIdentityProviderWithSchemeThatIsNotAUriScheme() {
        for (String walletScheme : new String[] {"alert(1)", " javascript ", "1wallet", "javascript:alert(1)"}) {
            try (Response response = realm.admin().identityProviders().create(idp(ALIAS, walletScheme))) {
                Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(),
                        "walletScheme '" + walletScheme + "' must be rejected");
            }
        }
    }

    @Test
    public void canCreateAndUpdateIdentityProviderWithCustomWalletScheme() {
        try (Response response = realm.admin().identityProviders().create(idp("wallet-scheme-custom", "haipd"))) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
        Assertions.assertEquals("haipd", storedWalletScheme("wallet-scheme-custom"));

        IdentityProviderResource resource = realm.admin().identityProviders().get("wallet-scheme-custom");
        IdentityProviderRepresentation rep = resource.toRepresentation();
        rep.getConfig().put(OID4VPIdentityProviderConfig.WALLET_SCHEME, "mdoc-openid4vp://");
        resource.update(rep);
        Assertions.assertEquals("mdoc-openid4vp://", storedWalletScheme("wallet-scheme-custom"));
    }

    @Test
    public void cannotUpdateIdentityProviderToJavascriptWalletScheme() {
        try (Response response = realm.admin().identityProviders().create(idp("wallet-scheme-update", "openid4vp"))) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        IdentityProviderResource resource = realm.admin().identityProviders().get("wallet-scheme-update");
        IdentityProviderRepresentation rep = resource.toRepresentation();
        rep.getConfig().put(OID4VPIdentityProviderConfig.WALLET_SCHEME, "javascript:alert(1)//");
        Assertions.assertThrows(ClientErrorException.class, () -> resource.update(rep));
        Assertions.assertEquals("openid4vp", storedWalletScheme("wallet-scheme-update"));
    }

    private String storedWalletScheme(String alias) {
        return realm.admin().identityProviders().get(alias).toRepresentation()
                .getConfig().get(OID4VPIdentityProviderConfig.WALLET_SCHEME);
    }

    private IdentityProviderRepresentation idp(String alias, String walletScheme) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(alias);
        idp.setProviderId(OID4VPIdentityProviderFactory.PROVIDER_ID);
        idp.setEnabled(true);
        Map<String, String> config = new HashMap<>();
        config.put(OID4VPIdentityProviderConfig.WALLET_SCHEME, walletScheme);
        config.put(OID4VPIdentityProviderConfig.TRUSTED_ISSUER_JWKS, "{\"keys\":[]}");
        idp.setConfig(config);
        return idp;
    }
}
