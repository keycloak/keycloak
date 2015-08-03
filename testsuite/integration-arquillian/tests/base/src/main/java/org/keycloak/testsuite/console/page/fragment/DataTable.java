package org.keycloak.testsuite.console.page.fragment;

import java.util.List;
import static org.keycloak.testsuite.util.SeleniumUtils.waitAjaxForElement;
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
    @FindBy(css = "tbody tr")
    private List<WebElement> rows;

    public void search(String pattern) {
        waitAjaxForElement(searchInput);
        searchInput.sendKeys(pattern);
        searchButton.click();
    }

    public void clickHeaderButton(String buttonText) {
        header.findElement(By.linkText(buttonText));
    }

    public WebElement body() {
        return body;
    }

    public List<WebElement> rows() {
        return rows;
    }

    public WebElement getRowByLinkText(String text) {
        WebElement row = body.findElement(By.xpath("//tr[//a[@text()='" + text + "']]"));
        waitAjaxForElement(row);
        return row;
    }

    public void clickActionButton(WebElement row, String buttonText) {
        row.findElement(xpath("//button[@text()='" + buttonText + "']"));
    }

}
