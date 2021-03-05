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

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.authentication.requiredactions.TermsAndConditions;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.TermsAndConditionsPage;
import org.keycloak.testsuite.util.UserBuilder;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TermsAndConditionsResetTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected TermsAndConditionsPage termsPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }


    @Before
    public void addTermsAndConditionsRequiredActionAndSecondUser() {
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        UserBuilder.edit(user).requiredAction(TermsAndConditions.PROVIDER_ID);
        adminClient.realm("test").users().get(user.getId()).update(user);

        RequiredActionProviderRepresentation rep = adminClient.realm("test").flows().getRequiredAction("terms_and_conditions");
        rep.setEnabled(true);
        adminClient.realm("test").flows().updateRequiredAction("terms_and_conditions", rep);

        createUser("test","test-user2@localhost", "password","terms_and_conditions");
        UserRepresentation user2 = ActionUtil.findUserWithAdminClient(adminClient, "test-user2@localhost");
    }

    @Test
    public void termsAcceptedAndRefreshed() {

        termsAndConditionsAccept("test-user@localhost");
        deleteAllSessionsInRealm("test");
        termsAndConditionsAccept("test-user2@localhost");
        deleteAllSessionsInRealm("test");

        adminClient.realm("test").flows().resetRequiredAction("terms_and_conditions");

        termsAndConditionsAccept("test-user@localhost");
        deleteAllSessionsInRealm("test");
        termsAndConditionsAccept("test-user2@localhost");
        deleteAllSessionsInRealm("test");

    }

    private void termsAndConditionsAccept(String userName){
        loginPage.open();
        loginPage.login(userName, "password");
        Assert.assertTrue(termsPage.isCurrent());
        termsPage.acceptTerms();
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, userName);
        List<String> userReqAct = user.getRequiredActions();
        assertTrue(userReqAct!=null && userReqAct.isEmpty());
    }

}
