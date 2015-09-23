/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.account;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;


/**
 *
 * @author vramik
 */
@Ignore //TODO find out the way how to restart browser during the test
public class RememberMeTest extends AbstractAccountManagementTest {

    
    @Before
    public void beforeRememberMe() {
        // enable remember me in test realm
        RealmRepresentation testRealmRep = testRealmResource().toRepresentation();
        testRealmRep.setRememberMe(true);
        testRealmResource().update(testRealmRep);
    }

    @Test
    public void rememberMe() {

        testRealmAccountManagementPage.navigateTo();
        
        log.info("login with remember me unchecked");
        testRealmLoginPage.form().login(testUser);
        testRealmAccountManagementPage.waitForAccountLinkPresent();
        log.debug("logged in");
        
        //TODO: restart browser
        
        testRealmAccountManagementPage.navigateTo();
        log.debug("user shouldn't be logged in");
        testRealmLoginPage.form().waitForRememberMePresent();
        
        log.info("login with remember me checked");
        testRealmLoginPage.form().rememberMe(true);
        testRealmLoginPage.form().login(testUser);
        testRealmAccountManagementPage.waitForAccountLinkPresent();
        
        //TODO: restart browser
        
        testRealmAccountManagementPage.navigateTo();
        log.debug("user should be logged in");
        testRealmAccountManagementPage.waitForAccountLinkPresent();
        
    }
    
}
