package org.keycloak.testsuite.ui.account2.page;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

public class MyResourcesPage extends AbstractLoggedInPage {

    @FindBy(xpath = "//ul[@id='resourcesList']/li")
    private List<WebElement> resourcesList;

    @Override
    public String getPageId() {
        return "resources";
    }

    public int getResourcesListCount() {
        return resourcesList.size();
    }

    public void clickExpandButton(int row) {
        driver.findElement(By.id("resourceToggle-" + row)).click();
    }

    public String getSharedWith(int row) {
        final WebElement element = driver.findElement(By.id("shared-with-user-message-" + row));
        return element.getText();
    }

    public void clickShareButton(int row) {
        driver.findElement(By.id("share-" + row)).click();
    }

    public void clickEditButton(int row) {
        final WebElement webElement = driver.findElement(By.id("action-menu-" + row));
        webElement.click();
        webElement.findElement(By.id("edit-" + row)).click();
    }

    public void createShare(String userName) {
        driver.findElement(By.id("username")).sendKeys(userName);
        driver.findElement(By.id("add")).click();
        driver.findElement(By.id("pf-toggle-id-6")).click();
        driver.findElement(By.id("Scope A-1")).click();
        driver.findElement(By.id("pf-toggle-id-9")).click();
        driver.findElement(By.id("done")).click();
    }

    public void removeAllPermissions() {
        driver.findElement(By.className("pf-c-select__toggle-clear")).click();
        driver.findElement(By.id("save-0")).click();
        driver.findElement(By.id("done")).click();
    }
}
