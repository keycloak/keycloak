package org.keycloak.testsuite.page.adapter;

import java.net.URL;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithProvidedUrl;
import org.keycloak.testsuite.util.SeleniumUtils;
import org.openqa.selenium.WebElement;

/**
 *
 * @author tkyjovsk
 */
public class CustomerPortalExample extends AbstractPageWithProvidedUrl {

    public static final String DEPLOYMENT_NAME = "customer-portal-example";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getProvidedUrl() {
        return url;
    }

    @FindByJQuery("h1:contains('Customer Portal')")
    private WebElement title;

    @FindByJQuery("a:contains('Customer Listing')")
    private WebElement customerListingLink;
    @FindByJQuery("h1:contains('Customer Listing')")
    private WebElement customerListingHeader;

    @FindByJQuery("a:contains('Customer Admin Interface')")
    private WebElement customerAdminInterfaceLink;

    @FindByJQuery("a:contains('Customer Sessions')")
    private WebElement customerSessionsLink;

    public void customerListing() {
        customerListingLink.click();
    }

    public void customerAdminInterface() {
        customerAdminInterfaceLink.click();
    }

    public void customerSessions() {
        customerSessionsLink.click();
    }

    public void waitForCustomerListingHeader() {
        SeleniumUtils.waitGuiForElementNotPresent(customerListingHeader);
    }

}
