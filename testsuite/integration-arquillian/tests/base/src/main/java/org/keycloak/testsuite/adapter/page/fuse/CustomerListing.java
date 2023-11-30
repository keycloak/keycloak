/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.adapter.page.fuse;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class CustomerListing extends CustomerPortalFuseExample {

    @Override
    public String getContext() {
        return super.getContext() + "/customers/cxf-rs.jsp";
    }

    @FindBy(linkText = "products")
    protected WebElement productsLink;
    @FindBy(linkText = "logout")
    protected WebElement logOutLink;
    @FindBy(linkText = "manage acct")
    protected WebElement accountManagementLink;

    public void clickProducts() {
        productsLink.click();
    }

    public void clickLogOut() {
        logOutLink.click();
    }

    public void clickAccountManagement() {
        accountManagementLink.click();
    }

}
