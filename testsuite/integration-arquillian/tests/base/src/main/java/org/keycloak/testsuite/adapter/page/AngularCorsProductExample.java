package org.keycloak.testsuite.adapter.page;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.openqa.selenium.WebElement;

import java.net.URL;

/**
 * Created by fkiss.
 */
public class AngularCorsProductExample extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "cors-angular-product-example";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        return url;
    }

    @FindByJQuery("button:contains('Reload')")
    private WebElement reloadDataButton;

    @FindByJQuery("button:contains('load Roles')")
    private WebElement loadRolesButton;

    @FindByJQuery("button:contains('Add Role')")
    private WebElement addRoleButton;

    @FindByJQuery("button:contains('Delete Role')")
    private WebElement deleteRoleButton;

    @FindByJQuery("button:contains('load available social providers')")
    private WebElement loadAvailableSocialProvidersButton;

    @FindByJQuery("button:contains('Load public realm info')")
    private WebElement loadPublicRealmInfoButton;

    @FindByJQuery("button:contains('Load version')")
    private WebElement loadVersionButton;

    public void reloadData() {
        reloadDataButton.click();
    }

    public void loadRoles() {
        loadRolesButton.click();
    }

    public void addRole() {
        addRoleButton.click();
    }

    public void deleteRole() {
        deleteRoleButton.click();
    }

    public void loadAvailableSocialProviders() {
        loadAvailableSocialProvidersButton.click();
    }

    public void loadPublicRealmInfo() {
        loadPublicRealmInfoButton.click();
    }

    public void loadVersion() {
        loadVersionButton.click();
    }


}