package org.keycloak.testsuite.console.page.fragment;

import org.jboss.arquillian.graphene.fragment.Root;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class Dropdown {
    @Root
    private WebElement dropDownRoot; // MUST be .kc-dropdown

    @FindBy(id = "kc-current-locale-link")
    private WebElement localeLink;

    @ArquillianResource
    private WebDriver driver;

    public String getSelected() {
        return localeLink.getText();
    }

    public void selectByText(String text) {
        localeLink.click();
        clickLink(dropDownRoot.findElement(By.xpath("./ul/li/a[text()='" + text + "']")));
    }
}
