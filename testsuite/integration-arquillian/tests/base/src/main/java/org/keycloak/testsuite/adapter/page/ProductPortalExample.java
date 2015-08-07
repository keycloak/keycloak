package org.keycloak.testsuite.adapter.page;

import java.net.URL;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.keycloak.testsuite.util.SeleniumUtils;
import org.openqa.selenium.WebElement;

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
        return url;
    }

    @FindByJQuery("h1:contains('Product Portal')")
    private WebElement title;

    @FindByJQuery("a:contains('Product Listing')")
    private WebElement productListingLink;
    @FindByJQuery("h1:contains('Product Listing')")
    private WebElement productListingHeader;

    @FindByJQuery("a:contains('customers')")
    private WebElement customersLink;

    public void productListing() {
        productListingLink.click();
    }

    public void goToCustomers() {
        customersLink.click();
    }

    public void waitForProductListingHeader() {
        SeleniumUtils.waitGuiForElementNotPresent(productListingHeader);
    }


}
