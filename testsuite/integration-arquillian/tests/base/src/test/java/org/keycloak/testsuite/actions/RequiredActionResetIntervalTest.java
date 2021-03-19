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

import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.authentication.requiredactions.TermsAndConditions;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.pages.*;

import java.util.*;


public class RequiredActionResetIntervalTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected LoginPage loginPage;

    @Page
    protected TermsAndConditionsPage termsPage;

    @Before
    public void setupRequiredActions() {

    }

    @Test
    public void testTermsAndConditionIntervalExpired() {

        //ensure user already has a pending required action
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        user.setRequiredActions(Collections.singletonList(TermsAndConditions.PROVIDER_ID));
        adminClient.realm("test").users().get(user.getId()).update(user);

        //configure required action reset interval
        RequiredActionProviderRepresentation rep = adminClient.realm("test").flows().getRequiredAction("terms_and_conditions");
        rep.setEnabled(true);
        Map<String, String> config = new HashMap<>();
        config.put("reset_every_value", "50");
        config.put("reset_every_unit", "MILLISECONDS");
        rep.setConfig(config);
        adminClient.realm("test").flows().updateRequiredAction(TermsAndConditions.PROVIDER_ID, rep);

        assertTrue(userHasPendingTerms());

        login("test-user@localhost", true);

        assertFalse(userHasPendingTerms());

        logout();

        try{
            Thread.sleep(700);
        }
        catch(InterruptedException ex){
            fail(ex.toString());
        }

        login("test-user@localhost", true);

    }


    @Test
    public void testTermsAndConditionIntervalNotExpired() {

        //configure required action reset interval
        RequiredActionProviderRepresentation rep = adminClient.realm("test").flows().getRequiredAction("terms_and_conditions");
        rep.setEnabled(true);
        Map<String, String> config = new HashMap<>();
        config.put("reset_every_value", "5000");
        config.put("reset_every_unit", "MILLISECONDS");
        rep.setConfig(config);
        adminClient.realm("test").flows().updateRequiredAction(TermsAndConditions.PROVIDER_ID, rep);

        //ensure user already has a pending required action
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        user.setRequiredActions(Collections.singletonList(TermsAndConditions.PROVIDER_ID));
        adminClient.realm("test").users().get(user.getId()).update(user);

        assertTrue(userHasPendingTerms());

        login("test-user@localhost", true);

        assertFalse(userHasPendingTerms());

        logout();

        login("test-user@localhost", false);

    }


    private boolean userHasPendingTerms(){
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        return user.getRequiredActions().contains(TermsAndConditions.PROVIDER_ID);
    }

    private void logout(){
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        adminClient.realm("test").users().get(user.getId()).logout();
    }

    private void login(String userName, boolean expectTerms) {
        loginPage.open();
        loginPage.login(userName, "password");
        if(expectTerms) {
            Assert.assertTrue(termsPage.isCurrent());
            termsPage.acceptTerms();
        }
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, userName);
        List<String> userReqAct = user.getRequiredActions();
        assertTrue(userReqAct!=null && userReqAct.isEmpty());
    }

}
