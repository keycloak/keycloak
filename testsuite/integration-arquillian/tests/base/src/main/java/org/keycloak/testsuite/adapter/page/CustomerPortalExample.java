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
public class CustomerPortalExample extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "customer-portal-example";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        //EAP6 URL fix
        URL fixedUrl = createInjectedURL("customer-portal");
        return fixedUrl != null ? fixedUrl : url;
    }

    @FindByJQuery("h1:contains('Customer Portal')")
    private WebElement title;

    @FindByJQuery("a:contains('Customer Listing')")
    private WebElement customerListingLink;
    @FindByJQuery("h1:contains('Customer Listing')")
    private WebElement customerListingHeader;

    @FindByJQuery("h1:contains('Customer Session')")
    private WebElement customerSessionHeader;

    @FindByJQuery("a:contains('Customer Admin Interface')")
    private WebElement customerAdminInterfaceLink;

    @FindByJQuery("a:contains('Customer Session')")
    private WebElement customerSessionLink;

    @FindByJQuery("a:contains('products')")
    private WebElement productsLink;

    @FindByJQuery("a:contains('logout')")
    private WebElement logOutButton;

    public void goToProducts() {
        productsLink.click();
    }

    public void customerListing() {
        customerListingLink.click();
    }

    public void customerAdminInterface() {
        customerAdminInterfaceLink.click();
    }

    public void customerSession() {
        waitUntilElement(customerSessionLink).is().present();
        customerSessionLink.click();
    }

    public void logOut() {
        logOutButton.click();
    }

    public void waitForCustomerListingHeader() {
        waitUntilElement(customerListingHeader).is().not().present();
    }

    public void waitForCustomerSessionHeader() {
        waitUntilElement(customerSessionHeader).is().not().present();
    }

}
