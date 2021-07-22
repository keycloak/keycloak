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
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.sessions.infinispan.changes.sessions.CrossDCLastSessionRefreshStoreFactory;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.services.scheduled.RequiredActionsResetTask;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;
import org.keycloak.testsuite.pages.*;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.timer.TimerProvider;

import java.util.*;
import java.util.stream.IntStream;


@AuthServerContainerExclude(AuthServer.REMOTE)
public class RequiredActionResetIntervalTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginPasswordUpdatePage changePasswordPage;

    @Page
    protected LoginUpdateProfileEditUsernameAllowedPage updateProfilePage;

    @Page
    protected TermsAndConditionsPage termsPage;

    @Before
    public void setupRequiredActions() {

        testingClient.server().run((session -> {
            //remove task for required actions
            TimerProvider timer = session.getProvider(TimerProvider.class);
            TimerProvider.TimerTaskContext context = timer.cancelTask("RequiredActionsResetTask");
            assertNotNull(context);
        }));

    }

    @Test
    public void testRequiredActionTnCIntervalReset() {

        //configure required action reset interval
        RequiredActionProviderRepresentation rep = adminClient.realm("test").flows().getRequiredAction("terms_and_conditions");
        rep.setEnabled(true);
        Map<String, String> config = new HashMap<>();
        config.put(RequiredActionsResetTask.INTERVAL_NUM, "1");
        config.put(RequiredActionsResetTask.UNIT_MULTIPLIER, "1");
        rep.setConfig(config);
        adminClient.realm("test").flows().updateRequiredAction("terms_and_conditions", rep);

        //ensure user has no pending required actions already
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        user.setRequiredActions(new ArrayList<>());
        adminClient.realm("test").users().get(user.getId()).update(user);


        testingClient.server().run((session -> {
            //add again the scheduled task for required actions
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.schedule(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new RequiredActionsResetTask(), 200), 200, "RequiredActionsResetTask");
        }));

        checkIfTermsAreReset();

        termsAndConditionsAccept("test-user@localhost");

        checkIfTermsAreReset();

    }

    private void checkIfTermsAreReset(){
        boolean requiredActionIsReset = false;
        for(int i=0;i<100;i++) {
            UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
            if(user.getRequiredActions().contains(TermsAndConditions.PROVIDER_ID))
                requiredActionIsReset = true;
            try{
                Thread.sleep(100);
            }
            catch(InterruptedException ex){
                break;
            }
        }
        assertTrue(requiredActionIsReset);
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
