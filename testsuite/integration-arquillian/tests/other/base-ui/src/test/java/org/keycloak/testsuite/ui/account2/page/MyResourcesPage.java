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
        //first row is the header
        final WebElement webElement = resourcesList.get(row + 1);

        //first button is the toggle second is share
        webElement.findElements(By.tagName("button")).get(1).click();
    }

    public void clickEditButton(int row) {
        //first row is the header
        final WebElement webElement = resourcesList.get(row + 1);

        //first button is the toggle second is share 3rd is the edit button
        webElement.findElements(By.tagName("button")).get(2).click();
    }

    public void createShare(String userName) {
        driver.findElement(By.id("username")).sendKeys(userName);
        driver.findElement(By.id("add")).click();
        driver.findElement(By.id("remove_pf-random-id-0")).click();
        driver.findElement(By.id("done")).click();
    }

    public void removeAllPermissions() {
        driver.findElement(By.id("remove_pf-random-id-0")).click();
        driver.findElement(By.id("remove_pf-random-id-1")).click();
        driver.findElement(By.id("done")).click();
    }
}
