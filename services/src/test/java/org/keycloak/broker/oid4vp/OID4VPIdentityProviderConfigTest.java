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

package org.keycloak.broker.oid4vp;

import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

/**
 * Validation of the walletScheme configuration: the scheme ends up as the href of the wallet
 * button on the login page, so schemes a browser executes or resolves locally (javascript,
 * data, ...) must be rejected when the identity provider is saved, and a scheme stored before
 * this validation existed must never reach the rendered wallet URL.
 */
public class OID4VPIdentityProviderConfigTest {

    private static final String JWKS = "{\"keys\":[]}";

    private OID4VPIdentityProviderConfig configWithWalletScheme(String walletScheme) {
        OID4VPIdentityProviderConfig config = new OID4VPIdentityProviderConfig();
        config.setAlias("oid4vp");
        config.setTrustedIssuerJwks(JWKS);
        if (walletScheme != null) {
            config.setWalletScheme(walletScheme);
        }
        return config;
    }

    @Test
    public void validateRejectsJavascriptWalletSchemeWithPayload() {
        // The exact value from the issue report: rendered as the wallet button href it
        // executes script in the Keycloak origin.
        OID4VPIdentityProviderConfig config = configWithWalletScheme("javascript:alert(document.domain)//");
        Assert.assertThrows(IllegalArgumentException.class, () -> config.validate(null));
    }

    @Test
    public void validateRejectsBareDangerousSchemes() {
        for (String walletScheme : new String[] {"javascript", "data", "vbscript", "blob", "file", "about"}) {
            Assert.assertThrows(IllegalArgumentException.class,
                    () -> configWithWalletScheme(walletScheme).validate(null));
        }
    }

    @Test
    public void validateRejectsDangerousSchemesWithSeparatorAndCaseVariants() {
        for (String walletScheme : new String[] {"javascript://", "JavaScript", "DATA://", "vbscript://"}) {
            Assert.assertThrows(IllegalArgumentException.class,
                    () -> configWithWalletScheme(walletScheme).validate(null));
        }
    }

    @Test
    public void validateRejectsWalletSchemeThatIsNotAUriScheme() {
        for (String walletScheme : new String[] {"alert(1)", " javascript ", "1wallet", "open id4vp",
                "javascript:alert(1)", "haipd://path"}) {
            Assert.assertThrows(IllegalArgumentException.class,
                    () -> configWithWalletScheme(walletScheme).validate(null));
        }
    }

    @Test
    public void validateAllowsBlankWalletSchemeAndAppliesTheDefault() {
        OID4VPIdentityProviderConfig config = configWithWalletScheme(null);
        config.validate(null);
        Assert.assertEquals("openid4vp://", config.getWalletScheme());
    }

    @Test
    public void validateAllowsCustomWalletSchemes() {
        for (String walletScheme : new String[] {"openid4vp", "mdoc-openid4vp://", "haipd", "https",
                "MyWallet-1.0+"}) {
            configWithWalletScheme(walletScheme).validate(null);
        }
    }

    @Test
    public void buildWalletUrlFallsBackToDefaultSchemeForStoredDangerousScheme() {
        // Config saved before the validation existed may still hold a dangerous scheme; the
        // rendered wallet URL must fall back to the default instead of emitting it.
        OID4VPIdentityProviderConfig config = configWithWalletScheme("javascript:alert(1)//");
        OID4VPIdentityProvider provider = new OID4VPIdentityProvider(null, config);

        String walletUrl = provider.buildWalletUrl("client-id", URI.create("https://localhost:8180/realms/test/request"));

        Assert.assertTrue(walletUrl, walletUrl.startsWith("openid4vp://?client_id=client-id"));
    }

    @Test
    public void buildWalletUrlUsesTheConfiguredCustomScheme() {
        OID4VPIdentityProviderConfig config = configWithWalletScheme("haipd");
        OID4VPIdentityProvider provider = new OID4VPIdentityProvider(null, config);

        String walletUrl = provider.buildWalletUrl("client-id", URI.create("https://localhost:8180/realms/test/request"));

        Assert.assertTrue(walletUrl, walletUrl.startsWith("haipd://?client_id=client-id"));
        Assert.assertTrue(walletUrl.contains("request_uri=https%3A%2F%2Flocalhost%3A8180%2Frealms%2Ftest%2Frequest"));
    }
}
