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

package org.keycloak.tests.conformance.vci;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.keycloak.representations.idm.oid4vc.VerifiableCredentialOfferActionConfig;
import org.keycloak.tests.conformance.containers.OpenIdConformanceSuite;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.keycloak.constants.OID4VCIConstants.VERIFIABLE_CREDENTIAL_OFFER_PROVIDER_ID;

/**
 * Creates a credential offer through the {@code verifiable_credential_offer} application initiated action, the
 * way an issuer application would: the holder logs in at a regular application client with the AIA parameter and
 * Keycloak mints the offer. The action config carries no client id, so the offer has no target client and the
 * wallet redeems it with its own client.
 *
 * <p>The login runs in a local headless Chrome. Keycloak is addressed at the hostname the suite containers reach
 * it at, which only resolves inside the container network, so the browser maps that hostname to loopback where
 * the managed Keycloak listens.
 */
final class VciAiaCredentialOffer {

    private VciAiaCredentialOffer() {
    }

    static String createOfferUri() {
        URI keycloakUri = OpenIdConformanceSuite.KEYCLOAK_BASE_URI;

        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--headless=new",
                "--disable-gpu",
                "--window-size=1920,1200",
                "--ignore-certificate-errors",
                "--disable-dev-shm-usage",
                "--remote-allow-origins=*",
                "--no-sandbox",
                "--host-resolver-rules=MAP " + keycloakUri.getHost() + " 127.0.0.1");
        WebDriver driver = new ChromeDriver(options);
        try {
            driver.get(authorizationUrl(keycloakUri));

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
            driver.findElement(By.id("username")).sendKeys(VciConformanceRealmConfig.HOLDER);
            driver.findElement(By.id("password")).sendKeys(VciConformanceRealmConfig.PASSWORD);
            driver.findElement(By.id("kc-login")).click();

            WebElement offerLink = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("credential-offer-uri-link")));
            String offer = offerLink.getDomAttribute("href");
            String[] parts = offer == null ? new String[0] : offer.split("credential_offer_uri=");
            if (parts.length < 2) {
                throw new IllegalStateException("No credential_offer_uri in AIA credential offer: " + offer);
            }
            return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
        } finally {
            driver.quit();
        }
    }

    private static String authorizationUrl(URI keycloakUri) {
        VerifiableCredentialOfferActionConfig actionConfig = new VerifiableCredentialOfferActionConfig();
        actionConfig.setCredentialConfigurationId(VciConformanceRealmConfig.CREDENTIAL_CONFIGURATION_ID);
        actionConfig.setPreAuthorized(false);

        String realmBase = keycloakUri + "/realms/" + VciConformanceRealmConfig.REALM;
        try {
            return realmBase + "/protocol/openid-connect/auth"
                    + "?client_id=" + VciConformanceRealmConfig.APP_CLIENT
                    + "&redirect_uri=" + encode(realmBase + "/account/")
                    + "&response_type=code&scope=openid"
                    + "&kc_action=" + encode(VERIFIABLE_CREDENTIAL_OFFER_PROVIDER_ID + ":" + actionConfig.asEncodedParameter());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode AIA credential offer action config", e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
