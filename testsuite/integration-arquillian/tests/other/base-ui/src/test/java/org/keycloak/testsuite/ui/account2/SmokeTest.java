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

package org.keycloak.testsuite.ui.account2;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.testsuite.ui.account2.page.DeviceActivityPage;
import org.keycloak.testsuite.ui.account2.page.PersonalInfoPage;

/**
 * Basic sanity check for Account Console
 *
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class SmokeTest extends AbstractAccountTest {
    @Page
    private PersonalInfoPage personalInfoPage;

    @Page
    private DeviceActivityPage deviceActivityPage;

    @Test
    public void baseFunctionalityTest() {
        accountWelcomeScreen.assertCurrent();
        accountWelcomeScreen.clickPersonalInfoLink();
        loginToAccount();
        personalInfoPage.assertCurrent();
        deviceActivityPage.navigateToUsingSidebar();
        deviceActivityPage.assertCurrent();
    }
}
