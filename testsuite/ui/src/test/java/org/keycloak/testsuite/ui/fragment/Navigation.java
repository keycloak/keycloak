package org.keycloak.testsuite.ui.fragment;
import static org.jboss.arquillian.graphene.Graphene.waitModel;
import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitGuiForElement;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;


public class Navigation {

    @FindByJQuery("a:contains('Settings')")
    private WebElement settingsLink;

    @FindByJQuery("a:contains('Users')")
    private WebElement usersLink;

    @FindByJQuery("a:contains('Roles')")
    private WebElement rolesLink;

    @FindByJQuery("a:contains('Applications')")
    private WebElement applicationsLink;

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

    //@FindByJQuery("a:contains('Themes')")
    @FindBy(css = ".nav-tabs > li:nth-child(7) > a:nth-child(1)")
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
	
    @FindByJQuery("div[id='content'] h2:visible")
    //@FindBy(css = "#content > div:nth-child(1) > h2:nth-child(1)")
    private WebElement currentHeader;

    public void settings() {
        openPage(settingsLink, "General Settings");
    }

    public void users() {
        openPage(usersLink, "Users");
    }

    public void roles() {
        openPage(rolesLink, "Realm-Level Roles");
    }

    public void applications() {
        openPage(applicationsLink, "Applications");
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
		openPage(loginLink, "Login Settings");
	}

    public void themes() { openPage(themesLink, "Theme Settings"); }

    public void roleMappings() {
		openPage(usersRoleMappings, "Role Mappings");
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
