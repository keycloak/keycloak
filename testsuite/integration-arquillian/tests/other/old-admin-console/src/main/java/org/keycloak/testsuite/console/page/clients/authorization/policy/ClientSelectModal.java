package org.keycloak.testsuite.console.page.clients.authorization.policy;

import org.keycloak.testsuite.console.page.fragment.MultipleStringSelect2;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.function.Function;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.openqa.selenium.By.tagName;

public class ClientSelectModal extends MultipleStringSelect2 {

    @Override
    protected List<WebElement> getSelectedElements() {
        return getRoot().findElements(By.xpath("(//select[@id='available-client'])/option"));
    }

    @Override
    protected Function<WebElement, String> representation() {
        return webElement -> getTextFromElement(webElement.findElements(tagName("td")).get(0));
    }
}
