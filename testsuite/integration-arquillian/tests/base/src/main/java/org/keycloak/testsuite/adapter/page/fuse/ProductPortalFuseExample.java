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
public class ProductPortalFuseExample extends AbstractFuseExample {

    public static final String DEPLOYMENT_NAME = "product-portal-fuse-example";
    public static final String DEPLOYMENT_CONTEXT = "product-portal";

    @Override
    public String getContext() {
        return DEPLOYMENT_CONTEXT;
    }

    @FindBy(linkText = "products")
    protected WebElement productsLink;
    @FindBy(linkText = "logout")
    protected WebElement logOutLink;
    @FindBy(linkText = "manage acct")
    protected WebElement accountManagementLink;

    @FindBy(xpath = "//p[contains(text(),'Product with ID 1 - unsecured request')]")
    protected WebElement product1Unsecured;
    @FindBy(xpath = "//p[contains(text(),'Product with ID 1 - secured request')]")
    protected WebElement product1Secured;
    @FindBy(xpath = "//p[contains(text(),'Product with ID 2 - secured request')]")
    protected WebElement product2Secured;

    public String getProduct1UnsecuredText() {
        return product1Unsecured.getText();
    }

    public String getProduct1SecuredText() {
        return product1Secured.getText();
    }

    public String getProduct2SecuredText() {
        return product2Secured.getText();
    }

    public void clickLogOutLink() {
        logOutLink.click();
    }

}
