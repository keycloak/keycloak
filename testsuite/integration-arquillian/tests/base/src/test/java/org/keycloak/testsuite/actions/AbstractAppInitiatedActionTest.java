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
package org.keycloak.testsuite.actions;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.WaitUtils;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.OAuth2Constants.REDIRECT_URI;
import static org.keycloak.OAuth2Constants.RESPONSE_TYPE;
import static org.keycloak.OAuth2Constants.SCOPE;
import static org.keycloak.models.Constants.CLIENT_ID;
import static org.keycloak.models.Constants.KC_ACTION;
import static org.keycloak.models.Constants.KC_ACTION_STATUS;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

/**
 * @author Stan Silvert
 */
public abstract class AbstractAppInitiatedActionTest extends AbstractTestRealmKeycloakTest {

    protected static final String SUCCESS = "success";
    protected static final String CANCELLED = "cancelled";

    @Page
    protected LoginPage loginPage;
    
    @Page
    protected AppPage appPage;
    
    @Rule
    public AssertEvents events = new AssertEvents(this);

    protected abstract String getAiaAction();
    
    protected void doAIA() {
        UriBuilder builder = OIDCLoginProtocolService.authUrl(authServerPage.createUriBuilder());
        String uri = builder.queryParam(KC_ACTION, getAiaAction())
                            .queryParam(RESPONSE_TYPE, "code")
                            .queryParam(CLIENT_ID, "test-app")
                            .queryParam(SCOPE, "openid")
                            .queryParam(REDIRECT_URI, getAuthServerContextRoot() + "/auth/realms/master/app/auth")
                            .build(TEST_REALM_NAME).toString();
        driver.navigate().to(uri);
        WaitUtils.waitForPageToLoad();
    }

    protected void assertKcActionStatus(String expectedStatus) {
        assertThat(appPage.getRequestType(),is(RequestType.AUTH_RESPONSE));

        final URI url;
        try {
            url = new URI(this.driver.getCurrentUrl());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        List<NameValuePair> pairs = URLEncodedUtils.parse(url, StandardCharsets.UTF_8);
        String kcActionStatus = null;
        for (NameValuePair p : pairs) {
            if (p.getName().equals(KC_ACTION_STATUS)) {
                kcActionStatus = p.getValue();
                break;
            }
        }
        assertThat(expectedStatus, is(kcActionStatus));
    }
    
    protected void assertSilentCancelMessage() {
        String url = this.driver.getCurrentUrl();
        assertThat("Expected no 'error=' in url", url, not(containsString("error=")));
        assertThat("Expected no 'error_description=' in url", url, not(containsString("error_description=")));
    }
}
