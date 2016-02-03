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

package org.keycloak.testsuite.adapter.page;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.openqa.selenium.WebElement;

import java.net.URL;

import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 *
 * @author tkyjovsk
 */
public class ProductPortalExample extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "product-portal-example";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        //EAP6 URL fix
        URL fixedUrl = createInjectedURL("product-portal");
        return fixedUrl != null ? fixedUrl : url;
    }

    @FindByJQuery("h1:contains('Product Portal')")
    private WebElement title;

    @FindByJQuery("a:contains('Product Listing')")
    private WebElement productListingLink;
    @FindByJQuery("h1:contains('Product Listing')")
    private WebElement productListingHeader;

    @FindByJQuery("a:contains('customers')")
    private WebElement customersLink;

    @FindByJQuery("a:contains('logout')")
    private WebElement logOutButton;

    public void productListing() {
        productListingLink.click();
    }

    public void goToCustomers() {
        customersLink.click();
    }

    public void waitForProductListingHeader() {
        waitUntilElement(productListingHeader).is().not().present();
    }

    public void logOut() {
        logOutButton.click();
    }


}
