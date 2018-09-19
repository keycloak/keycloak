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

package org.keycloak.testsuite.auth.page.account2.fragment;

import org.jboss.arquillian.graphene.fragment.Root;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.keycloak.testsuite.util.UIUtils.clickLink;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
// TODO rewrite this (blocked by KEYCLOAK-8217)
public class VerticalNavBar {
    @Root
    private WebElement navBarRoot;

    public void clickNavLinkByIndex(int i) {
        clickLink(getNavLinkByIndex(i));
    }

    public void clickSubNavLinkByIndex(int i1, int i2) {
        clickLink(getNavLinkByIndex(i1));
        clickLink(getSubNavLinkByIndex(i1, i2));
    }

    public boolean isNavLinkActive(int i) {
        return isNavLinkActive(getNavLinkByIndex(i));
    }

    public boolean isSubNavLinkActive(int i1, int i2) {
        return isNavLinkActive(getSubNavLinkByIndex(i1, i2));
    }

    private WebElement getNavLinkByIndex(int i) {
        return navBarRoot.findElement(By.xpath(String.format("./ul/li[%d]", i)));
    }

    private WebElement getSubNavLinkByIndex(int i1, int i2) {
        return navBarRoot.findElement(By.xpath(String.format("./ul/li[%d]/div[contains(@class,'nav-pf-secondary-nav')]/ul/li[%d]", i1, i2)));
    }

    private boolean isNavLinkActive(WebElement navLink) {
        return navLink.getAttribute("class").contains("active");
    }
}
