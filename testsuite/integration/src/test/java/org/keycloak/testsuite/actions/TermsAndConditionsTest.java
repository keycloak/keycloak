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
package org.keycloak.testsuite.actions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.authentication.requiredactions.TermsAndConditions;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.TermsAndConditionsPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TermsAndConditionsTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule();

    @Rule
    public WebRule webRule = new WebRule(this);

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected AppPage appPage;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected TermsAndConditionsPage termsPage;

    @Before
    public void before() {
        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel defaultRealm, RealmModel appRealm) {
                UserModel user = manager.getSession().users().getUserByUsername("test-user@localhost", appRealm);
                user.addRequiredAction(TermsAndConditions.PROVIDER_ID);
            }
        });
    }

    @Test
    public void termsAccepted() {
        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        termsPage.assertCurrent();

        termsPage.acceptTerms();

        String sessionId = events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION).removeDetail(Details.REDIRECT_URI).detail(Details.CUSTOM_REQUIRED_ACTION, TermsAndConditions.PROVIDER_ID).assertEvent().getSessionId();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().session(sessionId).assertEvent();

        // assert user attribute is properly set
        UserRepresentation user = keycloakRule.getUser("test", "test-user@localhost");
        Map<String,List<String>> attributes = user.getAttributesAsListValues();
        assertNotNull("timestamp for terms acceptance was not stored in user attributes", attributes);
        List<String> termsAndConditions = attributes.get(TermsAndConditions.USER_ATTRIBUTE);
        assertTrue("timestamp for terms acceptance was not stored in user attributes as "
                + TermsAndConditions.USER_ATTRIBUTE, termsAndConditions.size() == 1);
        String timestamp = termsAndConditions.get(0);
        assertNotNull("expected non-null timestamp for terms acceptance in user attribute "
                + TermsAndConditions.USER_ATTRIBUTE, timestamp);
        try {
            Integer.parseInt(timestamp);
        }
        catch (NumberFormatException e) {
            fail("timestamp for terms acceptance is not a valid integer: '" + timestamp + "'");
        }
    }

    @Test
    public void termsDeclined() {
        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        termsPage.assertCurrent();

        termsPage.declineTerms();

        events.expectLogin().event(EventType.CUSTOM_REQUIRED_ACTION_ERROR).detail(Details.CUSTOM_REQUIRED_ACTION, TermsAndConditions.PROVIDER_ID)
                .error(Errors.REJECTED_BY_USER)
                .removeDetail(Details.CONSENT)
                .assertEvent();


        // assert user attribute is properly removed
        UserRepresentation user = keycloakRule.getUser("test", "test-user@localhost");
        Map<String,List<String>> attributes = user.getAttributesAsListValues();
        if (attributes != null) {
            assertNull("expected null for terms acceptance user attribute " + TermsAndConditions.USER_ATTRIBUTE,
                    attributes.get(TermsAndConditions.USER_ATTRIBUTE));
        }
    }


}
