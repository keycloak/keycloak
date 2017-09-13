package org.keycloak.testsuite.console.page.fragment;

import org.jboss.arquillian.graphene.fragment.Root;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.keycloak.testsuite.util.URLUtils.navigateToUri;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class Dropdown {
    @Root
    private WebElement dropDownRoot; // MUST be .kc-dropdown

    @ArquillianResource
    private WebDriver driver;

    public String getSelected() {
        waitUntilElement(dropDownRoot).is().present();
        WebElement element = dropDownRoot.findElement(By.xpath("./a"));
        return element.getText();
    }

    public void selectByText(String text) {
        waitUntilElement(dropDownRoot).is().present();
        WebElement element = dropDownRoot.findElement(By.xpath("./ul/li/a[text()='" + text + "']"));
        navigateToUri(element.getAttribute("href"), true); // TODO: move cursor to show the menu and then click the menu item
    }
}
