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

package org.keycloak.testsuite.ui.account2.page.fragment;

import org.jboss.arquillian.graphene.fragment.Root;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.openqa.selenium.WebDriver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class Sidebar extends AbstractFragmentWithMobileLayout {
    public static int MOBILE_WIDTH = 767; // if the page width is less or equal than this, we expect the sidebar to be collapsed by default
    public static final String NAV_ITEM_ID_PREFIX = "nav-link-";

    @Drone
    protected WebDriver driver;

    @Root
    private WebElement sidebarRoot;

    @Override
    protected int getMobileWidth() {
        return MOBILE_WIDTH;
    }

    public boolean isCollapsed() {
        return sidebarRoot.getAttribute("class").contains("collapsed");
    }

    public void collapse() {
        assertFalse("Sidebar is already collapsed", isCollapsed());
        getCollapseToggle().click();
        pause(2000); // wait for animation
        assertTrue("Sidebar is not collapsed", isCollapsed());
    }

    public void expand() {
        assertTrue("Sidebar is already expanded", isCollapsed());
        getCollapseToggle().click();
        pause(2000); // wait for animation
        assertFalse("Sidebar is not expanded", isCollapsed());
    }

    private WebElement getCollapseToggle(){
        return driver.findElement(By.id("nav-toggle"));
    }

    protected void performOperationWithSidebarExpanded(Runnable operation) {
        if (isMobileLayout()) expand();
        operation.run();
        if (isMobileLayout()) collapse();
    }

    protected WebElement getNavElement(String id) {
        return sidebarRoot.findElement(By.id(NAV_ITEM_ID_PREFIX + id));
    }

    public void assertNavNotPresent(String id) {
        try {
            getNavElement(id).isDisplayed();
            throw new AssertionError("Nav element " + id + " shouldn't be present");
        }
        catch (NoSuchElementException e) {
            // ok
        }
    }

    protected WebElement getNavSubsection(WebElement parent) {
        return parent.findElement(By.xpath("../section[@aria-labelledby='" + parent.getAttribute("id") + "']"));
    }

    public void clickNav(String id) {
        performOperationWithSidebarExpanded(() -> clickLink(getNavElement(id)));
    }

    public void clickSubNav(String parentId, String id) {
        performOperationWithSidebarExpanded(() -> {
            WebElement parentNavItem = getNavElement(parentId);
            if (!isNavSubsectionExpanded(parentNavItem)) {
                parentNavItem.click();
            }
            WebElement navItem = getNavSubsection(parentNavItem).findElement(By.id(NAV_ITEM_ID_PREFIX + id));
            clickLink(navItem);
        });
    }

    public boolean isNavSubsectionExpanded(String parentId) {
        return isNavSubsectionExpanded(getNavElement(parentId));
    }

    protected boolean isNavSubsectionExpanded(WebElement parent) {
        return getNavSubsection(parent).getAttribute("hidden") == null;
    }

    public String getActiveNavId() {
        List<WebElement> activeNavElements = sidebarRoot.findElements(
                By.xpath("//*[starts-with(@id,'" + NAV_ITEM_ID_PREFIX + "') and contains(@class,'current')]")
        );

        assertEquals("There are more than 1 active nav items", 1, activeNavElements.size());

        return activeNavElements.get(0).getAttribute("id").split(NAV_ITEM_ID_PREFIX)[1];
    }
}
