package org.keycloak.testsuite.ui.page.settings;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.ui.model.Application;
import org.keycloak.testsuite.ui.page.AbstractPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;
import org.jboss.arquillian.graphene.findby.ByJQuery;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitAjaxForElement;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.By.linkText;
import static org.openqa.selenium.By.tagName;

/**
 * Created by fkiss.
 */
public class ApplicationPage extends AbstractPage {

    @FindBy(id = "name")
    private WebElement nameInput;

    @FindBy(id = "")
    private WebElement enabledSwitchToggle;

    @FindBy(id = "accessType")
    private WebElement accessTypeDropDownMenu;

    @FindBy(id = "newRedirectUri")
    private WebElement redirectUriInput;

    @FindBy(css = "table[class*='table']")
    private WebElement dataTable;

    @FindBy(css = "button.kc-icon-search.ng-scope")
    private WebElement searchButton;

    @FindBy(css = "input[class*='search']")
    private WebElement searchInput;

    @FindByJQuery("button[data-ng-click*='addRedirectUri']")
    private WebElement addUriButton;

    @FindByJQuery("button[data-ng-click*='deleteRedirectUri']")
    private WebElement deleteUriButton;

    @FindByJQuery("li[ng-class*='credentials']>a")
    private WebElement credentialsTab;

    @FindByJQuery("li[ng-class*='roles']>a")
    private WebElement rolesTab;

    @FindByJQuery("li[ng-class*='claims']>a")
    private WebElement claimsTab;

    @FindByJQuery("li[ng-class*='scope']>a")
    private WebElement scopeTab;

    @FindByJQuery("li[ng-class*='revocation']>a")
    private WebElement revocationTab;

    @FindByJQuery("li[ng-class*='sessions']>a")
    private WebElement sessionsTab;

    @FindByJQuery("li[ng-class*='installation']>a")
    private WebElement instalationTab;

    public void addApplication(Application application) {
        primaryButton.click();
        waitAjaxForElement(nameInput);
        nameInput.sendKeys(application.getName());
        if (!application.isEnabled()) {
            enabledSwitchToggle.click();
        }
        accessTypeDropDownMenu.sendKeys(application.getAccessType());
        redirectUriInput.sendKeys(application.getUri());
        addUriButton.click();
        primaryButton.click();
    }

    public void addApplicationWithoutUri(Application application) {
        primaryButton.click();
        waitAjaxForElement(nameInput);
        nameInput.sendKeys(application.getName());
        if (!application.isEnabled()) {
            enabledSwitchToggle.click();
        }
        accessTypeDropDownMenu.sendKeys(application.getAccessType());
    }

    public void addUri(Application application) {
        redirectUriInput.sendKeys(application.getUri());
        addUriButton.click();
    }

    public void addUri(String uri) {
        redirectUriInput.sendKeys(uri);
        addUriButton.click();
    }

    public void removeUri(Application application) {
        //redirectUriInput.sendKeys(application.getUri());
    }

    public void confirmAddApplication() {
        primaryButton.click();
    }

    public void deleteApplication(String applicationName) {
        searchInput.sendKeys(applicationName);
        searchButton.click();
        driver.findElement(linkText(applicationName)).click();
        waitAjaxForElement(dangerButton);
        dangerButton.click();
        waitAjaxForElement(deleteConfirmationButton);
        deleteConfirmationButton.click();
    }

    public Application findApplication(String applicationName) {
		waitAjaxForElement(searchInput);
		searchInput.sendKeys(applicationName);
        searchButton.click();
        List<Application> applications = getAllRows();
        if(applications.isEmpty()) {
            return null;

        } else {
            assertEquals(1, applications.size());
            return applications.get(0);
        }
    }

    private List<Application> getAllRows() {
        List<Application> rows = new ArrayList<Application>();
        for (WebElement rowElement : dataTable.findElements(cssSelector("tbody tr"))) {
            Application app = new Application();
            List<WebElement> tds = rowElement.findElements(tagName("td"));
            if(!(tds.isEmpty() || tds.get(0).getText().isEmpty())) {
                app.setName(tds.get(0).getText());
                app.setUri(tds.get(2).getText());
                rows.add(app);
            }
        }
        return rows;
    }
	
	public void goToCreateApplication() {
		driver.findElements(ByJQuery.selector(".btn.btn-primary")).get(1).click();
	}
}