/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;
import org.keycloak.testsuite.util.UserBuilder;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;

/**
 * Only covers basic use cases for App Initialized actions. Complete dynamic user profile behavior is tested in {@link RequiredActionUpdateProfileWithUserProfileTest} as it shares same code as the App initialized action.
 *
 * @author Stan Silvert
 */
public class AppInitiatedActionVerifyEmailTest extends AbstractAppInitiatedActionTest {

    @Override
    public String getAiaAction() {
        return UserModel.RequiredAction.VERIFY_EMAIL.name();
    }
    
    @Page
    protected VerifyEmailPage verifyEmailPage;

    @Page
    protected ErrorPage errorPage;
    
    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void beforeTest() {
        ApiUtil.removeUserByUsername(testRealm(), "test-user@localhost");
        UserRepresentation user = UserBuilder.create().enabled(true)
                .username("test-user@localhost")
                .email("test-user@localhost")
                .firstName("Tom")
                .lastName("Brady")
                .build();
        ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, "password");
    }
  
    @Test
    public void sendVerifyEmail() {
        doAIA();

        loginPage.login("test-user@localhost", "password");
        
        verifyEmailPage.assertCurrent();

    }
    
    @Test
    public void cancelUpdateProfile() {
        doAIA();

        loginPage.login("test-user@localhost", "password");
        
        verifyEmailPage.assertCurrent();
        verifyEmailPage.cancel();

        assertKcActionStatus(CANCELLED);

    }

}
