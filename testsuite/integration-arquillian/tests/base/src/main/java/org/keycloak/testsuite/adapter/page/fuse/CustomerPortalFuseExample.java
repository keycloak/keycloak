package org.keycloak.testsuite.adapter.page.fuse;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class CustomerPortalFuseExample extends AbstractFuseExample {

    public static final String DEPLOYMENT_NAME = "customer-portal-fuse-example";
    public static final String DEPLOYMENT_CONTEXT = "customer-portal";

    @Override
    public String getContext() {
        return DEPLOYMENT_CONTEXT;
    }

    @FindBy(linkText = "Customer Listing - CXF RS endpoint")
    protected WebElement customerListingLink;

    @FindBy(linkText = "Admin Interface - Apache Camel endpoint")
    protected WebElement adminInterfaceLink;

    public void clickCustomerListingLink() {
        customerListingLink.click();
    }

    public void clickAdminInterfaceLink() {
        adminInterfaceLink.click();
    }
    
}
