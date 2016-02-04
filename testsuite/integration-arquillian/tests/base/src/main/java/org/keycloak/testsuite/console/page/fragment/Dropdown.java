package org.keycloak.testsuite.console.page.fragment;

import org.jboss.arquillian.graphene.fragment.Root;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class Dropdown {
    @Root
    private WebElement dropDownRoot; // MUST be .kc-dropdown

    @ArquillianResource
    private WebDriver driver;

    public String getSelected() {
        return dropDownRoot.findElement(By.xpath("./a")).getText();
    }

    public void selectByText(String text) {
        String href = dropDownRoot.findElement(By.xpath("./ul/li/a[text()='" + text + "']")).getAttribute("href");
        driver.navigate().to(href);
    }
}
