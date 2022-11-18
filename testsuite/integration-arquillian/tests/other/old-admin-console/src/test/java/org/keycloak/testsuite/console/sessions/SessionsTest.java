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
package org.keycloak.testsuite.console.sessions;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.sessions.RealmSessions;

import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;

/**
 *
 * @author Petr Mensik
 */
@Ignore
public class SessionsTest extends AbstractConsoleTest {

    @Page
    private RealmSessions realmSessionsPage;
    
    @Test
    public void testLogoutAllSessions() {
        loginToTestRealmConsoleAs(testUser);
        
        // back to master admin console
        adminConsoleRealmPage.navigateTo();
        manage().sessions();
        realmSessionsPage.realmSessions();

        realmSessionsPage.logoutAllSessions();

        // verify test user was logged out from the admin console
        testRealmAdminConsolePage.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }
    
}
