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

import org.junit.Test;
import org.keycloak.testsuite.ui.account2.page.AbstractLoggedInPage;
import org.keycloak.testsuite.ui.account2.page.utils.SigningInPageUtils;

import static org.junit.Assert.assertTrue;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class BaseAccountPageTest extends AbstractAccountTest {
    protected abstract AbstractLoggedInPage getAccountPage();

    @Override
    public void navigateBeforeTest() {
        getAccountPage().navigateTo();
        loginToAccount();
        getAccountPage().assertCurrent();
    }

    @Test
    public void navigationTest() {
        pageNotFound.navigateTo();
        pageNotFound.assertCurrent();

        getAccountPage().navigateToUsingSidebar();
        getAccountPage().assertCurrent();

        if (getAccountPage().getParentPageId() != null) {
            assertTrue("Nav bar subsection should be expanded after clicking nav item",
                    getAccountPage().sidebar().isNavSubsectionExpanded(getAccountPage().getParentPageId()));
        }
    }

    protected void testModalDialog(Runnable triggerModal, Runnable onCancel) {
        SigningInPageUtils.testModalDialog(getAccountPage(), triggerModal, onCancel);
    }
}
