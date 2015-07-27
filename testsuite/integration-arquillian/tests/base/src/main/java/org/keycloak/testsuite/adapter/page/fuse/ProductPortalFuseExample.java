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

    @FindBy(xpath = "//p[contains(text(),'Product with ID 1 - unsecured request')]")
    public WebElement product1Unsecured;
    @FindBy(xpath = "//p[contains(text(),'Product with ID 1 - secured request')]")
    public WebElement product1Secured;
    @FindBy(xpath = "//p[contains(text(),'Product with ID 2 - secured request')]")
    public WebElement product2Secured;

    @FindBy(linkText = "products")
    public WebElement productsLink;
    @FindBy(linkText = "logout")
    public WebElement logOutLink;
    @FindBy(linkText = "manage acct")
    public WebElement accountManagementLink;

}
