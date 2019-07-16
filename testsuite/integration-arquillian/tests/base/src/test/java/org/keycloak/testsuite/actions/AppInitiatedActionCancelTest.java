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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.pages.LoginUpdateProfileEditUsernameAllowedPage;

/**
 * Test makes sure that sending a cancel signal does not remove a non-AIA
 * required action
 *
 * @author Stan Silvert
 */
public class AppInitiatedActionCancelTest extends AbstractAppInitiatedActionTest {
    
    @Page
    protected LoginUpdateProfileEditUsernameAllowedPage updateProfilePage;
    
    public AppInitiatedActionCancelTest() {
        super("update_profile");
    }
    
    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        ActionUtil.addRequiredActionForUser(testRealm, "test-user@localhost", UserModel.RequiredAction.UPDATE_PROFILE.name());
    }
    
    @Test
    // Verify that sending a "cancel" does not remove the required action.
    public void cancelUpdateProfile() {
        doAIA();
        loginPage.login("test-user@localhost", "password");
        updateProfilePage.assertCurrent();
        updateProfilePage.cancel();
        assertRedirectSuccess();
        
        appPage.logout();
        
        loginPage.open();
        loginPage.assertCurrent();
        loginPage.login("test-user@localhost", "password");
        updateProfilePage.assertCurrent();
    }
}
