package org.keycloak.testsuite.console.page.fragment;

import java.util.List;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitAjaxForElement;
import org.openqa.selenium.By;
import static org.openqa.selenium.By.xpath;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class DataTable {

    @FindBy(css = "input[class*='search']")
    private WebElement searchInput;
    @FindBy(css = "div[class='input-group-addon'] i")
    private WebElement searchButton;

    @FindBy(tagName = "thead")
    private WebElement header;
    @FindBy(css = "tbody")
    private WebElement body;
    @FindBy(css = "tbody tr.ng-scope")
    private List<WebElement> rows;
    
    @FindBy
    private WebElement infoRow;

    public void search(String pattern) {
        waitAjaxForBody();
        searchInput.sendKeys(pattern);
        searchButton.click();
    }

    public void clickHeaderButton(String buttonText) {
        waitAjaxForBody();
        header.findElement(By.xpath(".//button[text()='" + buttonText + "']")).click();
    }

    public void clickHeaderLink(String linkText) {
        waitAjaxForBody();
        header.findElement(By.linkText(linkText)).click();
    }

    public WebElement body() {
        return body;
    }

    public void waitAjaxForBody() {
        waitAjaxForElement(body);
    }

    public List<WebElement> rows() {
        waitAjaxForBody();
        pause(250);
        return rows;
    }

    public WebElement getRowByLinkText(String text) {
        WebElement row = body.findElement(By.xpath(".//tr[./td/a[text()='" + text + "']]"));
        waitAjaxForElement(row);
        return row;
    }

    public void clickRowByLinkText(String text) {
        body.findElement(By.xpath(".//tr/td/a[text()='" + text + "']")).click();
    }

    public void clickRowActionButton(WebElement row, String buttonText) {
        row.findElement(xpath(".//button[text()='" + buttonText + "']")).click();
    }
    
}
