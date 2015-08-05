package org.keycloak.testsuite.console.page.fragment;

import java.util.List;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class Breadcrumb {

    @FindBy(xpath = "/li[not contains(@class,'ng-hide')]")
    private List<WebElement> items;

    public int size() {
        return items.size();
    }

    public WebElement getItem(int index) {
        return items.get(index);
    }

    public WebElement getItemFromEnd(int index) {
        return items.get(size() - index);
    }

}
