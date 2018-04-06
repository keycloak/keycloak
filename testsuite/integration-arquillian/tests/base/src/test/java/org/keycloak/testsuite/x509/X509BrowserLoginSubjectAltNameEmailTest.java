/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel;
import org.keycloak.events.Details;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.x509.X509IdentityConfirmationPage;

/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @date 8/12/2016
 */

public class X509BrowserLoginSubjectAltNameEmailTest extends AbstractX509AuthenticationTest {

    @Page
    protected AppPage appPage;

    @Page
    protected X509IdentityConfirmationPage loginConfirmationPage;

    @Page
    protected LoginPage loginPage;

    @BeforeClass
    public static void onBeforeTestClass() {
        if (Boolean.parseBoolean(System.getProperty("auth.server.jboss"))) {
            String authServerHome = System.getProperty("auth.server.home");

            if (authServerHome != null && System.getProperty("auth.server.ssl.required") != null) {
                authServerHome = authServerHome + "/standalone/configuration";
                StringBuilder cliArgs = new StringBuilder();

                cliArgs.append("--ignore-ssl-errors=true ");
                cliArgs.append("--web-security=false ");
                cliArgs.append("--ssl-certificates-path=" + authServerHome + "/ca.crt ");
                cliArgs.append("--ssl-client-certificate-file=" + authServerHome + "/certs/clients/test-user-san-email@localhost.cert.pem ");
                cliArgs.append("--ssl-client-key-file=" + authServerHome + "/certs/clients/test-user@localhost.key.pem ");
                cliArgs.append("--ssl-client-key-passphrase=password");

                System.setProperty("keycloak.phantomjs.cli.args", cliArgs.toString());
            }
        }
    }

    private void login(X509AuthenticatorConfigModel config, String userId, String username, String attemptedUsername) {

        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        loginConfirmationPage.open();

        Assert.assertTrue(loginConfirmationPage.getSubjectDistinguishedNameText().equals("CN=test-user, OU=Keycloak, O=Red Hat, L=Boston, ST=MA, C=US"));
        Assert.assertEquals(username, loginConfirmationPage.getUsernameText());

        loginConfirmationPage.confirm();

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin()
                .user(userId)
                .detail(Details.USERNAME, attemptedUsername)
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();
    }

    @Test
    public void loginAsUserFromCertSubjectEmail() {
        login(createLoginSubjectAltNameEmail2UsernameOrEmailConfig(), userId, "test-user@localhost", "test-user@localhost");
    }
}