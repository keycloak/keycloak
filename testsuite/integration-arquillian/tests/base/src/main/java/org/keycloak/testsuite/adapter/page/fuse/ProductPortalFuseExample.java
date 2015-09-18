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
