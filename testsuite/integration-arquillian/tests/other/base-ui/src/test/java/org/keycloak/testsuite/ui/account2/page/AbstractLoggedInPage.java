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

package org.keycloak.testsuite.ui.account2.page;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.ui.account2.page.fragment.ContentAlert;
import org.keycloak.testsuite.ui.account2.page.fragment.ContinueCancelModal;
import org.keycloak.testsuite.ui.account2.page.fragment.LoggedInPageHeader;
import org.keycloak.testsuite.ui.account2.page.fragment.Sidebar;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.LinkedList;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class AbstractLoggedInPage extends AbstractAccountPage {
    public static final String ACCOUNT_SECURITY_ID = "security";

    @FindBy(xpath = "//*[@id='main_react_container']//header")
    private LoggedInPageHeader header;

    @FindBy(id = "page-sidebar")
    private Sidebar sidebar;

    @Page
    private ContentAlert alert;

    @Page
    private ContinueCancelModal modal;

    @FindBy(xpath = ".//*[@id='page-heading']//h1")
    private WebElement pageTitle;

    @FindBy(id = "refresh-page")
    private WebElement refreshPageBtn;
    
    @FindBy(id = "brandLink")
    private WebElement brandLink;

    public AbstractLoggedInPage() {
        hashPath = new LinkedList<>();
        if (getParentPageId() != null) hashPath.add(getParentPageId());
        hashPath.add(getPageId());
    }

    /**
     * This is currently used only by navigation menu to identify nav items. See content.json in themes module for IDs.
     *
     * @return page ID
     */
    public abstract String getPageId();

    /**
     * In case the page is placed is a subpage, i.e. placed in a subsection. See content.json in themes module for IDs.
     *
     * @return parent page ID
     */
    public String getParentPageId() {
        return null;
    }

    /**
     * This should simulate a user navigating to this page using links in the nav bar. It assume that user is logged in
     * and at some Account Console page (not Welcome Screen), i.e. that the nav bar is visible.
     */
    public void navigateToUsingSidebar() {
        if (getParentPageId() != null) {
            sidebar().clickSubNav(getParentPageId(), getPageId());
        }
        else {
            sidebar().clickNav(getPageId());
        }
    }

    public LoggedInPageHeader header() {
        return header;
    }

    public Sidebar sidebar() {
        return sidebar;
    }

    public ContentAlert alert() {
        return alert;
    }

    public ContinueCancelModal modal() {
        return modal;
    }

    public String getPageTitle() {
        return getTextFromElement(pageTitle);
    }

    public void clickRefreshPage() {
        clickLink(refreshPageBtn);
    }

    public void clickBrandLink() {
        clickLink(brandLink);
    }

    @Override
    public boolean isCurrent() {
        return super.isCurrent() && getPageId().equals(sidebar().getActiveNavId());
    }
}
