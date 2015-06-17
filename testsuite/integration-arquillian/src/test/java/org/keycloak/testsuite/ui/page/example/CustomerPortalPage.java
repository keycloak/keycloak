package org.keycloak.testsuite.ui.page.example;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.ui.util.SeleniumUtils;
import org.openqa.selenium.WebElement;

/**
 *
 * @author tkyjovsk
 */
public class CustomerPortalPage extends ExampleAppPage {

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
