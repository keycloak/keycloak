/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.federation.kerberos;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.authentication.authenticators.browser.SpnegoAuthenticatorFactory;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.federation.kerberos.KerberosConfig;
import org.keycloak.federation.kerberos.KerberosFederationProviderFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.KerberosEmbeddedServer;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.util.KerberosRule;
import org.keycloak.testsuite.pages.*;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.Cookie;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;

import org.keycloak.common.constants.KerberosConstants;

/**:q
 * Test for the KerberosFederationProvider (kerberos without LDAP integration)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosAlternativeTest extends AbstractKerberosSingleRealmTest {

    private static final String PROVIDER_CONFIG_LOCATION = "classpath:kerberos/kerberos-standalone-connection.properties";

    @ClassRule
    public static KerberosRule kerberosRule = new KerberosRule(PROVIDER_CONFIG_LOCATION, KerberosEmbeddedServer.DEFAULT_KERBEROS_REALM);


    @Override
    protected KerberosRule getKerberosRule() {
        return kerberosRule;
    }


    @Before
    public void before() {
        // don't run this test when map storage is enabled, as map storage doesn't support the legacy style federation
        ProfileAssume.assumeFeatureDisabled(Profile.Feature.MAP_STORAGE);
    }

    @Before
    public void setupFlows() {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");

            if (realm.getBrowserFlow().getAlias().equals("kerberos-alternative-flow")) {
                return;
            }

            // Parent flow
            AuthenticationFlowModel browser = new AuthenticationFlowModel();
            browser.setAlias("kerberos-alternative-flow");
            browser.setDescription("browser kerberos alternative authentication");
            browser.setProviderId("basic-flow");
            browser.setTopLevel(true);
            browser.setBuiltIn(true);
            browser = realm.addAuthenticationFlow(browser);
            realm.setBrowserFlow(browser);

            // Subflow1
            AuthenticationFlowModel subflow1 = new AuthenticationFlowModel();
            subflow1.setTopLevel(false);
            subflow1.setBuiltIn(true);
            subflow1.setAlias("subflow-1");
            subflow1.setDescription("Default alternative username+password");
            subflow1.setProviderId("basic-flow");
            subflow1 = realm.addAuthenticationFlow(subflow1);

            AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
            execution.setParentFlow(browser.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            execution.setFlowId(subflow1.getId());
            execution.setPriority(10);
            execution.setAuthenticatorFlow(true);
            realm.addAuthenticatorExecution(execution);


            // Subflow1 - username password
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(subflow1.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(UsernameFormFactory.PROVIDER_ID);
            execution.setPriority(20);
            execution.setAuthenticatorFlow(false);

            realm.addAuthenticatorExecution(execution);



            // Subflow2
            AuthenticationFlowModel subflow2 = new AuthenticationFlowModel();
            subflow2.setTopLevel(false);
            subflow2.setBuiltIn(true);
            subflow2.setAlias("subflow-2");
            subflow2.setDescription("Alternative Kerberos");
            subflow2.setProviderId("basic-flow");
            subflow2 = realm.addAuthenticationFlow(subflow2);

            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(browser.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            execution.setFlowId(subflow2.getId());
            execution.setPriority(20);
            execution.setAuthenticatorFlow(true);
            realm.addAuthenticatorExecution(execution);

            // Subflow2 - username-password
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(subflow2.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(SpnegoAuthenticatorFactory.PROVIDER_ID);
            execution.setPriority(20);
            execution.setAuthenticatorFlow(false);

            realm.addAuthenticatorExecution(execution);

        });
    }


    @Override
    protected CommonKerberosConfig getKerberosConfig() {
        return new KerberosConfig(getUserStorageConfiguration());
    }

    @Override
    protected ComponentRepresentation getUserStorageConfiguration() {
        return getUserStorageConfiguration("kerberos-standalone", KerberosFederationProviderFactory.PROVIDER_NAME);
    }


    @Page
    protected SelectAuthenticatorPage selectAuthenticatorPage;


    private BasicCookieStore cookieStore;
    @Override
    protected void initHttpClient(boolean useSpnego) {
        if (client != null) {
            cleanupApacheHttpClient();
        }
        cookieStore = new BasicCookieStore();

        HttpClient httpClient = new HttpClientBuilder()
                .setDefaultCookieStore(cookieStore)
                .spNegoSchemeFactory(spnegoSchemeFactory)
                .useSPNego(useSpnego)
                .build();

        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient);
        client = ((ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()).httpEngine(engine).build();
    }

    @Test
    public void spnegoLoginTest() throws Exception {

        String expectedUsername = "hnelson";
        String username = "hnelson";
        String password = "secret";

        loginPage.open();
        loginPage.assertCurrent();
        loginPage.assertTryAnotherWayLinkAvailability(true);
        loginPage.clickTryAnotherWayLink();
        WaitUtils.waitForPageToLoad();
        selectAuthenticatorPage.assertCurrent();
        assertThat(selectAuthenticatorPage.getLoginMethodHelpText(SelectAuthenticatorPage.SPNEGO),
                is("auth-spnego-help-text"));

        // Request for SPNEGO login sent with Resteasy client

        // save form url, dataform and cookies to replay it by resteasy
        String spnegoUrl = selectAuthenticatorPage.getFormActionUrl();
        Entity spnegoFormData = selectAuthenticatorPage.getFormData(SelectAuthenticatorPage.SPNEGO);
        Set<Cookie> cookies = selectAuthenticatorPage.getCookies();
        for (Cookie cookie: cookies) {
            var c = new BasicClientCookie(cookie.getName(),cookie.getValue());
            c.setDomain(cookie.getDomain());
            c.setPath(cookie.getPath());
            cookieStore.addCookie(c);
        }

        // set login password
        spnegoSchemeFactory.setCredentials(username, password);


        // replay like browser same request with spnego Resteasy client
        Response spnegoResponse = client.target(spnegoUrl).request().post(spnegoFormData);
        if (spnegoResponse.getStatus() == 302) {
            if (spnegoResponse.getLocation() != null) {
                String uri = spnegoResponse.getLocation().toString();
                if (uri.contains("login-actions/required-action") || uri.contains("auth_session_id")) {
                    spnegoResponse = client.target(uri).request().get();
                }
            }
        }
        Assert.assertEquals(302, spnegoResponse.getStatus());

        List<UserRepresentation> users = testRealmResource().users().search(expectedUsername, 0, 1);
        String userId = users.get(0).getId();
        events.expectLogin()
                .client("kerberos-app")
                .user(userId)
                .detail(Details.USERNAME, expectedUsername)
                .assertEvent();

        // Assert user was imported and hasn't any required action on him. Profile info is NOT synced from LDAP. Just username is filled and email is "guessed"
        assertUser(expectedUsername, expectedUsername + "@" + kerberosRule.getConfig().get(KerberosConstants.KERBEROS_REALM).toLowerCase(), null, null, false);
    }

}
