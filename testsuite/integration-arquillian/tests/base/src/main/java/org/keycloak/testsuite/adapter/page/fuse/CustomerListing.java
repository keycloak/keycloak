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
