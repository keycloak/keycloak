/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.url;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.containers.AbstractQuarkusDeployableContainer;
import org.keycloak.testsuite.arquillian.containers.RemoteContainer;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.util.RealmBuilder;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_PORT;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SCHEME;
import static org.keycloak.testsuite.util.oauth.OAuthClient.AUTH_SERVER_ROOT;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * This is testing just the V2 implementation of Hostname SPI. It is NOT testing if the Hostname SPI as such is used correctly.
 * It is NOT testing that correct URL types are used at various places in Keycloak.
 *
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class HostnameV2Test extends AbstractKeycloakTest {
    private static final String realmFrontendName = "frontendUrlRealm";
    private static final String realmFrontendUrl = "https://realmFrontend.localtest.me:445";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation customHostname = RealmBuilder.create().name(realmFrontendName)
                .attribute("frontendUrl", realmFrontendUrl)
                .build();
        testRealms.add(customHostname);
    }

    @Test
    public void testFixedFrontendHostname() {
        String hostname = "localtest.me";
        String dynamicUrl = getDynamicBaseUrl(hostname);

        updateServerHostnameSettings(hostname, null, false, true);

        testFrontendAndBackendUrls("master", dynamicUrl, dynamicUrl);
        testAdminUrls("master", dynamicUrl, dynamicUrl);
    }

    @Test
    public void testFixedFrontendHostnameUrl() {
        String fixedUrl = "https://localtest.me:444";

        updateServerHostnameSettings(fixedUrl, null, false, true);

        testFrontendAndBackendUrls("master", fixedUrl, fixedUrl);
        testAdminUrls("master", fixedUrl, fixedUrl);
    }

    @Test
    public void testFixedFrontendAndAdminHostnameUrl() {
        String fixedFrontendUrl = "http://localtest.me:444";
        String fixedAdminUrl = "https://admin.localtest.me:445";

        updateServerHostnameSettings(fixedFrontendUrl, fixedAdminUrl, false, true);

        testFrontendAndBackendUrls("master", fixedFrontendUrl, fixedFrontendUrl);
        testAdminUrls("master", fixedFrontendUrl, fixedAdminUrl);
    }

    @Test
    public void testFixedFrontendHostnameUrlWithDefaultPort() {
        String fixedFrontendUrl = "https://localtest.me";
        String fixedAdminUrl = "https://admin.localtest.me";

        updateServerHostnameSettings("https://localtest.me:443", "https://admin.localtest.me:443", false, true);

        testFrontendAndBackendUrls("master", fixedFrontendUrl, fixedFrontendUrl);
        testAdminUrls("master", fixedFrontendUrl, fixedAdminUrl);
    }

    @Test
    public void testDynamicBackend() {
        String fixedUrl = "https://localtest.me:444";

        updateServerHostnameSettings(fixedUrl, null, true, true);

        testFrontendAndBackendUrls("master", fixedUrl, AUTH_SERVER_ROOT);
        testAdminUrls("master", fixedUrl, fixedUrl);
    }

    @Test
    public void testDynamicEverything() {
        updateServerHostnameSettings(null, null, false, false);

        testFrontendAndBackendUrls("master", AUTH_SERVER_ROOT, AUTH_SERVER_ROOT);
        testAdminUrls("master", AUTH_SERVER_ROOT, AUTH_SERVER_ROOT);
    }

    @Test
    public void testRealmFrontendUrlWithOtherUrlsSet() {
        String fixedFrontendUrl = "https://localtest.me:444";
        String fixedAdminUrl = "https://admin.localtest.me:445";

        updateServerHostnameSettings(fixedFrontendUrl, fixedAdminUrl, true, true);

        testFrontendAndBackendUrls(realmFrontendName, realmFrontendUrl, AUTH_SERVER_ROOT);
        testAdminUrls(realmFrontendName, realmFrontendUrl, fixedAdminUrl);
    }

    @Test
    public void testRealmFrontendUrl() {
        updateServerHostnameSettings("localtest.me", null, false, true);

        testFrontendAndBackendUrls(realmFrontendName, realmFrontendUrl, realmFrontendUrl);
        testAdminUrls(realmFrontendName, realmFrontendUrl, realmFrontendUrl);
    }

    @Test
    public void testStrictMode() {
        testStartupFailure("hostname is not configured; either configure hostname, or set hostname-strict to false",
                null, null, null, true);
    }

//    @Test
//    public void testStrictModeMustBeDisabledWhenHostnameIsSpecified() {
//        testStartupFailure("hostname is configured, hostname-strict must be set to true",
//                "localtest.me", null, null, false);
//    }

    @Test
    public void testInvalidHostnameUrl() {
        testStartupFailure("Provided hostname is neither a plain hostname nor a valid URL",
                "htt://localtest.me", null, null, true);
    }

    @Test
    public void testInvalidAdminUrl() {
        testStartupFailure("Provided hostname-admin is not a valid URL",
                "localtest.me", "htt://admin.localtest.me", null, true);
    }

    @Test
    public void testBackchannelDynamicRequiresHostname() {
        testStartupFailure("hostname-backchannel-dynamic must be set to false when no hostname is provided",
                null, null, true, false);
    }

    @Test
    public void testBackchannelDynamicRequiresFullHostnameUrl() {
        testStartupFailure("hostname-backchannel-dynamic must be set to false if hostname is not provided as full URL",
                "localtest.me", null, true, true);
    }

    private String getDynamicBaseUrl(String hostname) {
        return AUTH_SERVER_SCHEME + "://" + hostname + ":" + AUTH_SERVER_PORT + "/auth";
    }

    private void testFrontendAndBackendUrls(String realm, String expectedFrontendUrl, String expectedBackendUrl) {
        OIDCConfigurationRepresentation config = oauth.realm(realm).doWellKnownRequest();
        assertEquals(expectedFrontendUrl + "/realms/" + realm, config.getIssuer());
        assertEquals(expectedFrontendUrl + "/realms/" + realm + "/protocol/openid-connect/auth", config.getAuthorizationEndpoint());
        assertEquals(expectedBackendUrl + "/realms/" + realm + "/protocol/openid-connect/token", config.getTokenEndpoint());
        assertEquals(expectedBackendUrl + "/realms/" + realm + "/protocol/openid-connect/userinfo", config.getUserinfoEndpoint());
    }

    private void testAdminUrls(String realm, String expectedFrontendUrl, String expectedAdminUrl) {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            String adminIndexPage = SimpleHttpDefault.doGet(AUTH_SERVER_ROOT + "/admin/" + realm + "/console", client).asString();
            assertThat(adminIndexPage, containsString("\"authServerUrl\": \"" + expectedFrontendUrl +"\""));
            assertThat(adminIndexPage, containsString("\"authUrl\": \"" + expectedAdminUrl +"\""));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void testStartupFailure(String expectedError, String hostname, String hostnameAdmin, Boolean hostnameBackchannelDynamic, Boolean hostnameStrict) {
        String errorLog = "";
        DeployableContainer<?> container = suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer();

        try {
            updateServerHostnameSettings(hostname, hostnameAdmin, hostnameBackchannelDynamic, hostnameStrict);
            Assert.fail("Server didn't fail");
        }
        catch (Exception e) {
            if (container instanceof RemoteContainer) {
                errorLog = ((RemoteContainer) container).getRemoteLog();
            }
            else {
                errorLog = ExceptionUtils.getStackTrace(e);
            }
        }

        // need to start the server back again to perform standard after test cleanup
        resetHostnameSettings();
        try {
            container.stop(); // just to make sure all components are stopped (useful for Undertow)
            container.start();
            reconnectAdminClient();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertThat(errorLog, containsString(expectedError));
    }

    private void updateServerHostnameSettings(String hostname, String hostnameAdmin, Boolean hostnameBackchannelDynamic, Boolean hostnameStrict) {
        try {
            suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer().stop();
            setHostnameOptions(hostname, hostnameAdmin, hostnameBackchannelDynamic, hostnameStrict);
            suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer().start();
            reconnectAdminClient();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setHostnameOptions(String hostname, String hostnameAdmin, Boolean hostnameBackchannelDynamic, Boolean hostnameStrict) {
        if (suiteContext.getAuthServerInfo().isQuarkus()) {
            List<String> args = new ArrayList<>();
            if (hostname != null) {
                args.add("--hostname=" + hostname);
            }
            if (hostnameAdmin != null) {
                args.add("--hostname-admin=" + hostnameAdmin);
            }
            if (hostnameBackchannelDynamic != null) {
                args.add("--hostname-backchannel-dynamic=" + hostnameBackchannelDynamic);
            }
            if (hostnameStrict != null) {
                args.add("--hostname-strict=" + hostnameStrict);
            }

            AbstractQuarkusDeployableContainer container = (AbstractQuarkusDeployableContainer) suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer();
            container.setAdditionalBuildArgs(args);
        }
        else {
            setConfigProperty("keycloak.hostname", hostname);
            setConfigProperty("keycloak.hostname-admin", hostnameAdmin);
            setConfigProperty("keycloak.hostname-backchannel-dynamic", hostnameBackchannelDynamic == null ? null : String.valueOf(hostnameBackchannelDynamic));
            setConfigProperty("keycloak.hostname-strict", hostnameStrict == null ? null : String.valueOf(hostnameStrict));
        }
    }

    @After
    public void resetHostnameSettings() {
        if (suiteContext.getAuthServerInfo().isQuarkus()) {
            AbstractQuarkusDeployableContainer container = (AbstractQuarkusDeployableContainer) suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer();
            container.resetConfiguration();
        }
        else {
            setHostnameOptions(null, null, null, null);
            setConfigProperty("keycloak.hostname.provider", null);
        }
    }

    private static void setConfigProperty(String name, String value) {
        if (value != null) {
            System.setProperty(name, value);
        }
        else {
            System.clearProperty(name);
        }
    }
}
