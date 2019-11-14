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
import org.junit.Assert;
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
import java.util.List;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;

/**
 * @author Stan Silvert
 */
public abstract class AbstractAppInitiatedActionTest extends AbstractTestRealmKeycloakTest {
    
    @Page
    protected LoginPage loginPage;
    
    @Page
    protected AppPage appPage;
    
    @Rule
    public AssertEvents events = new AssertEvents(this);
    
    protected final String aiaAction;
    
    public AbstractAppInitiatedActionTest(String aiaAction) {
        this.aiaAction = aiaAction;
    }
    
    protected void doAIA() {
        UriBuilder builder = OIDCLoginProtocolService.authUrl(authServerPage.createUriBuilder());
        String uri = builder.queryParam("kc_action", this.aiaAction)
                            .queryParam("response_type", "code")
                            .queryParam("client_id", "test-app")
                            .queryParam("scope", "openid")
                            .queryParam("redirect_uri", AuthServerTestEnricher.getAuthServerContextRoot() + "/auth/realms/master/app/auth")
                            .build(TEST_REALM_NAME).toString();
        driver.navigate().to(uri);
        WaitUtils.waitForPageToLoad();
    }

    protected void assertKcActionStatus(String expectedStatus) {
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        URI url = null;
        try {
            url = new URI(this.driver.getCurrentUrl());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        List<NameValuePair> pairs = URLEncodedUtils.parse(url, "UTF-8");
        String kcActionStatus = null;
        for (NameValuePair p : pairs) {
            if (p.getName().equals("kc_action_status")) {
                kcActionStatus = p.getValue();
                break;
            }
        }
        Assert.assertEquals(expectedStatus, kcActionStatus);
    }
    
    protected void assertSilentCancelMessage() {
        String url = this.driver.getCurrentUrl();
        Assert.assertFalse("Expected no 'error=' in url", url.contains("error="));
        Assert.assertFalse("Expected no 'error_description=' in url", url.contains("error_description="));
    }
}
