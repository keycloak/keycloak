package org.keycloak.testsuite.ui.fragment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import static org.jboss.arquillian.graphene.Graphene.waitModel;
import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitGuiForElement;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;


public class Navigation {
	
	@Drone
	private WebDriver driver;

    @FindByJQuery("a:contains('Settings')")
    private WebElement settingsLink;

    @FindByJQuery("a:contains('Users')")
    private WebElement usersLink;

    @FindByJQuery("a:contains('Roles')")
    private WebElement rolesLink;

    @FindByJQuery("a:contains('Clients')")
    private WebElement clientsLink;

    @FindByJQuery("a:contains('OAuth')")
    private WebElement oauthLink;

    @FindByJQuery("a:contains('Sessions')")
    private WebElement sessionsLink;

    @FindByJQuery("a:contains('Security')")
    private WebElement securityLink;

    @FindByJQuery("a:contains('Events')")
    private WebElement eventsLink;
	
	@FindByJQuery("a:contains('Login')")
    private WebElement loginLink;

    @FindByJQuery("a:contains('Themes')")
    private WebElement themesLink;

	@FindByJQuery("a:contains('Role Mappings')")
    private WebElement usersRoleMappings;
	
	@FindByJQuery("a:contains('Timeout Settings')")
    private WebElement timeoutSettings;
	
	@FindByJQuery("a:contains('Add Realm')")
    private WebElement addRealm;
	
	@FindByJQuery("a:contains('Credentials')")
    private WebElement credentials;
	
	@FindByJQuery("a:contains('Attributes')")
    private WebElement attributes;
	
    @FindBy(css = "div h1")
    private WebElement currentHeader;

	public void selectRealm(String realmName) {
		driver.findElement(By.linkText(realmName)).click();
	}
	
    public void settings() {
        openPage(settingsLink, "Settings");
    }

    public void users() {
        openPage(usersLink, "Users");
    }

    public void roles() {
        openPage(rolesLink, "Roles");
    }

    public void clients() {
        openPage(clientsLink, "Clients");
    }
	
    public void oauth() {
        openPage(oauthLink, "OAuth Clients");
    }

    public void sessions() {
        openPage(sessionsLink, "Total Active Sessions");
    }

    public void security() {
        openPage(securityLink, "Browser Security Headers");
    }

    public void events() {
        openPage(eventsLink, "Events");
    }
	
	public void login() {
		openPage(loginLink, "Settings");
	}

    public void themes() { openPage(themesLink, "Theme Settings"); }

    public void roleMappings() {
		openPage(usersRoleMappings, "User");
	}
	
	public void timeoutSettings() {
		openPage(timeoutSettings, "Timeout Settings");
	}
	
	public void addRealm() {
		openPage(addRealm, "Add Realm");
	}
	
	public void credentials() {
		openPage(credentials, "Credentials");
	}
	
	public void attributes() {
		openPage(attributes, "Attributes");
	}

    private void openPage(WebElement page, String headerText) {
        waitGuiForElement(page);
        page.click();
        waitModel().until().element(currentHeader).text().contains(headerText);
    }
}
