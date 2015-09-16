package org.keycloak.testsuite.console.page.fragment;

import java.util.List;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class Breadcrumb {

    public static final String BREADCRUMB_XPATH = "//ol[@class='breadcrumb']";

    @FindBy(xpath = "./li[not(contains(@class,'ng-hide'))]/a")
    private List<WebElement> items;

    public int size() {
        return items.size();
    }

    public WebElement getItem(int index) {
        return items.get(index);
    }

    public WebElement getItemFromEnd(int index) {
        return items.get(size() - index - 1);
    }

    public void clickItemOneLevelUp() {
        getItemFromEnd(0).click();
    }

}
