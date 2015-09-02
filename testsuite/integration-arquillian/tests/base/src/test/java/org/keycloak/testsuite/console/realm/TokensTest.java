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
package org.keycloak.testsuite.console.realm;

import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.console.page.realm.TokenSettings;

import org.jboss.arquillian.graphene.page.Page;
import static org.keycloak.testsuite.util.LoginAssert.assertCurrentUrlStartsWithLoginUrlOf;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;

/**
 *
 * @author Petr Mensik
 */
public class TokensTest extends AbstractRealmTest {

    @Page
    private TokenSettings tokenSettings;

    private static final int TIMEOUT = 4;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    @Before
    public void beforeTokensTest() {
        configure().realmSettings();
        tabs().tokens();
    }

    @Test
    public void testTimeoutForRealmSession() throws InterruptedException {
        tokenSettings.form().setSessionTimeout(TIMEOUT, TIME_UNIT);
        tokenSettings.form().save();

        loginToTestRealmConsoleAs(testRealmUser);
        TIME_UNIT.sleep(TIMEOUT + 2);
        
        driver.navigate().refresh();

        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
    }

    @Test
    public void testLifespanOfRealmSession() throws InterruptedException {
        tokenSettings.form().setSessionTimeoutLifespan(TIMEOUT, TIME_UNIT);
        tokenSettings.form().save();
        
        loginToTestRealmConsoleAs(testRealmUser);
        TIME_UNIT.sleep(TIMEOUT/2);

        driver.navigate().refresh();
        assertCurrentUrlStartsWith(testRealmAdminConsole); // assert still logged in (within lifespan)
        
        TIME_UNIT.sleep(TIMEOUT/2 + 2);
        driver.navigate().refresh();

        assertCurrentUrlStartsWithLoginUrlOf(testRealm); // assert logged out (lifespan exceeded)
    }
}
