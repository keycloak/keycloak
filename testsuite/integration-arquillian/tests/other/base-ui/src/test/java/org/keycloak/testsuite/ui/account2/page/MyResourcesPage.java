package org.keycloak.testsuite.ui.account2.page;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import static org.junit.Assert.assertEquals;

public class MyResourcesPage extends AbstractLoggedInPage {

    @FindBy(xpath = "//ul[@id='resourcesList']/li")
    private List<WebElement> resourcesList;

    @FindBy(id = "refresh-page")
    private WebElement refreshButton;

    @Override
    public String getPageId() {
        return "resources";
    }

    public int getResourcesListCount() {
        return resourcesList.size();
    }

    public void clickRefreshButton() {
        refreshButton.click();
    }

    public void clickExpandButton(int row) {
        driver.findElement(By.id("resourceToggle-" + row)).click();
        waitGui().until().element(By.id("ex-expand" + row)).is().visible();
    }

    public void clickCollapseButton(int row) {
        driver.findElement(By.id("resourceToggle-" + row)).click();
        waitGui().until().element(By.id("ex-expand" + row)).is().not().visible();
    }

    public String getCellText(String cell, int row) {
        return getCell(cell, row).getText();
    }

    public String getCellHref(String cell, int row) {
        return getCell(cell, row).findElement(By.tagName("a")).getAttribute("href");
    }

    private WebElement getCell(String cell, int row) {
        final String name = Character.toUpperCase(cell.charAt(0)) + cell.substring(1);
        return driver.findElement(By.id(String.format("resource%s-%d", name, row)));
    }

    public String getSharedWith(int row) {
        final WebElement element = driver.findElement(By.id("shared-with-user-message-" + row));
        return element.getText();
    }

    public void clickShareButton(int row) {
        driver.findElement(By.id("share-" + row)).click();
        waitForModalFadeIn();
    }

    public void clickEditButton(int row) {
        final WebElement webElement = driver.findElement(By.id("action-menu-" + row));
        webElement.click();
        webElement.findElement(By.id("edit-" + row)).click();
        waitForModalFadeIn();
    }

    public String getEditDialogUsername(int row) {
        return driver.findElement(By.id("username-" + row)).getAttribute("value");
    }

    public void clickRemoveButton(int row) {
        final WebElement webElement = driver.findElement(By.id("action-menu-" + row));
        webElement.click();
        webElement.findElement(By.id("remove-" + row)).click();
    }

    public String getPendingRequestRequestor(int row) {
        return driver.findElement(By.id("requestor" + row)).getText();
    }

    public String getPendingRequestPermissions(int row) {
        return driver.findElement(By.id("permissions" + row)).getText();
    }

    private WebElement getPendingRequest(String resourceName) {
        return driver.findElement(By.id("shareRequest-" + resourceNameToId(resourceName)));
    }

    public String getPendingRequestText(String resourceName) {
        return getPendingRequest(resourceName).getText();
    }

    public void clickPendingRequest(String resourceName) {
        getPendingRequest(resourceName).click();
    }

    public void acceptRequest(String resourceName, int row) {
        clickApproveDenyButton(resourceName, row, true);
    }

    public void denyRequest(String resourceName, int row) {
        clickApproveDenyButton(resourceName, row, false);
    }

    private void clickApproveDenyButton(String resourceName, int row, boolean approve) {
        final By id = By.id(String.format("%s-%d-shareRequest-%s", approve ? "accept" : "deny", row, resourceNameToId(resourceName)));
        driver.findElement(id).click();
        waitForModalFadeOut();
    }

    private String resourceNameToId(String resourceName) {
        return resourceName.replace(" ", "-");
    }

    public void clickSignOut() {
        driver.findElement(By.id("signOutButton")).click();
    }

    public void clickNextPage() {
        final WebElement webElement = driver.findElements(By.className("pf-m-primary")).get(1);
        assertEquals("Next>", webElement.getText());
        webElement.click();
    }

    public void clickSharedWithMeTab() {
        final WebElement sharedWithMe = driver.findElement(By.id("pf-tab-1-sharedwithMe"));
        sharedWithMe.click();

        final WebElement tab = sharedWithMe.findElement(By.xpath("./.."));
        //test to see that the tab is really clicked
        assertEquals("pf-c-tabs__item pf-m-current", tab.getAttribute("class"));
    }

    public boolean containsResource(String resourceName) {
        return driver.findElement(By.id("sharedResourcesList")).getText().contains(resourceName);
    }

    public void createShare(String userName) {
        driver.findElement(By.id("username")).sendKeys(userName);
        driver.findElement(By.id("add")).click();
        driver.findElement(By.id("pf-toggle-id-6")).click();
        driver.findElement(By.id("Scope A-1")).click();
        driver.findElement(By.id("pf-toggle-id-9")).click();
        driver.findElement(By.id("done")).click();
        waitForModalFadeOut();
    }

    public void removeAllPermissions() {
        List<String> buttonTexts = Arrays.asList(getScopeText("0"), getScopeText("1"));
        assertThat(buttonTexts, containsInAnyOrder("Scope A", "Scope B"));
        driver.findElement(By.className("pf-c-select__toggle-clear")).click();
        driver.findElement(By.id("save-0")).click();
        driver.findElement(By.id("done")).click();
        waitForModalFadeOut();
    }

    private String getScopeText(String id) {
        return driver.findElement(By.id(String.format("pf-random-id-%s", id))).getText();
    }

    private void waitForModalFadeIn() {
        waitGui().until().element(By.className("pf-c-modal-box")).is().present();
    }

    private void waitForModalFadeOut() {
        waitGui().until().element(By.className("pf-c-backdrop")).is().not().present();
    }
}
