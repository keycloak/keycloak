package org.keycloak.testsuite.adapter.page;

import java.net.URL;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.WebElement;

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
        return url;
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
        WaitUtils.waitGuiForElement(customerSessionLink);
        customerSessionLink.click();
    }

    public void logOut() {
        logOutButton.click();
    }

    public void waitForCustomerListingHeader() {
        WaitUtils.waitGuiForElementNotPresent(customerListingHeader);
    }

    public void waitForCustomerSessionHeader() {
        WaitUtils.waitGuiForElementNotPresent(customerSessionHeader);
    }

}
