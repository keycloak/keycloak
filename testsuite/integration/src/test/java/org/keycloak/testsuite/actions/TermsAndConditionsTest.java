/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.pages.TermsAndConditionsPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

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

    }


}
