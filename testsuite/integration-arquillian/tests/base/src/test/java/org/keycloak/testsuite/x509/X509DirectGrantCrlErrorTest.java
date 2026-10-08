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

package org.keycloak.testsuite.x509;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.HtmlUnitBrowser;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

public class X509DirectGrantCrlErrorTest extends AbstractX509AuthenticationTest {

    private static final String UNPARSABLE_CRL = "unparsable-test.crl";
    private static final String STALE_CRL = "stale-test.crl";
    private static final String MISSING_CRL = "missing-test.crl";

    @ClassRule
    public static CRLRule crlRule = new CRLRule();

    @Drone
    @HtmlUnitBrowser
    private WebDriver htmlUnit;

    private Path unparsableCrl;
    private Path staleCrl;

    @Before
    public void replaceTheDefaultDriver() {
        replaceDefaultWebDriver(htmlUnit);
    }

    @Before
    public void createCrlFiles() throws Exception {
        // Not possible to test file CRL on undertow at this moment - jboss config dir doesn't exist
        ContainerAssume.assumeNotAuthServerUndertow();
        Path configDir = Paths.get(testingClient.server().fetch(CrlFileLoaderProbe.configDir(), String.class));
        unparsableCrl = Files.writeString(configDir.resolve(UNPARSABLE_CRL), "not a CRL", StandardCharsets.UTF_8);
        staleCrl = Files.write(configDir.resolve(STALE_CRL), CrlGenerator.generateStaleCrl());
    }

    @After
    public void deleteCrlFiles() throws Exception {
        if (unparsableCrl != null) {
            Files.deleteIfExists(unparsableCrl);
        }
        if (staleCrl != null) {
            Files.deleteIfExists(staleCrl);
        }
    }

    @Test
    public void unparsableCrlFileDetailsAreNotReturned() {
        assertCrlFailure(UNPARSABLE_CRL, "Unable to load CRL");
    }

    @Test
    public void missingCrlFileDetailsAreNotReturned() {
        assertCrlFailure(MISSING_CRL, "Unable to load CRL");
    }

    @Test
    public void staleCrlFileDetailsAreNotReturned() {
        assertCrlFailure(STALE_CRL, "CRL is not refreshed");
    }

    @Test
    public void missingHttpCrlDetailsAreNotReturned() {
        assertCrlFailure(CRLRule.CRL_RESPONDER_ORIGIN + "/missing.crl", "Unable to load CRL");
    }

    private void assertCrlFailure(String crlPath, String expectedDescription) {
        X509AuthenticatorConfigModel config = new X509AuthenticatorConfigModel()
                .setCRLEnabled(true)
                .setCRLRelativePath(crlPath)
                .setCrlAbortIfNonUpdated(true);
        String configId = createConfig(directGrantExecution.getId(), newConfig("x509-crl", config.getConfig()));
        Assert.assertNotNull(configId);

        oauth.client("resource-owner", "secret");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        Assert.assertEquals(401, response.getStatusCode());
        Assert.assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        Assert.assertEquals(expectedDescription, response.getErrorDescription());
    }
}
