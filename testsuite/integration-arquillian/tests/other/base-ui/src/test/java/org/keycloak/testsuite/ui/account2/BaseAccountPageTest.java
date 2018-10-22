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

package org.keycloak.testsuite.ui.account2;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.auth.page.account2.AbstractLoggedInPage;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class BaseAccountPageTest extends AbstractAccountTest {
    protected abstract AbstractLoggedInPage getAccountPage();

    @Before
    public void beforeBaseAccountPageTest() {
        getAccountPage().navigateTo();
        loginToAccount();
        getAccountPage().assertCurrent();
    }

    @Test
    @Ignore // TODO, blocked by KEYCLOAK-8217
    public void navigationTest() {
        getAccountPage().navigateToUsingNavBar();
        getAccountPage().assertCurrent();
    }
}
