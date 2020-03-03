package org.keycloak.testsuite.ui.account2.page;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

public class MyResourcesPage extends AbstractLoggedInPage {

    @FindBy(id = "resourcesList")
    private List<WebElement> resourcesList;

    @Override
    public String getPageId() {
        return "resources";
    }

    public int getResourcesListCount() {
        return resourcesList.size();
    }
}
