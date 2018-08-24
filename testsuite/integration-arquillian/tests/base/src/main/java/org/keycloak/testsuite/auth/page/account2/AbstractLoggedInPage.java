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

package org.keycloak.testsuite.auth.page.account2;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.auth.page.account2.fragment.VerticalNavBar;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class AbstractLoggedInPage extends AbstractAccountPage {
    @Page
    protected WelcomeScreen welcomeScreen;

    @FindBy(className = "nav-pf-vertical nav-pf-vertical-with-badges")
    private VerticalNavBar verticalNavBar;

    @FindBy(id = "pageTitle")
    protected WebElement pageTitle;

    @Override
    protected List<String> createHashPath() {
        return new ArrayList<>();
    }

    /**
     * This should simulate a user navigating to this page using links in the nav bar. It assume that user is logged in
     * and at some Account Console page (not Welcome Screen), i.e. that the nav bar is visible.
     */
    public abstract void navigateToUsingNavBar();

    public VerticalNavBar verticalNavBar() {
        return verticalNavBar;
    }
}
