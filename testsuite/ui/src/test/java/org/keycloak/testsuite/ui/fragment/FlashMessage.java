package org.keycloak.testsuite.ui.fragment;

import org.jboss.arquillian.graphene.fragment.Root;

import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitGuiForElement;
import org.openqa.selenium.WebElement;

public class FlashMessage {

    @Root
    private WebElement root;

    public boolean isSuccess() {
        return root.getAttribute("class").contains("success");
    }

    public boolean isError() {
        return root.getAttribute("class").contains("error");
    }

    public boolean isDanger() {
		return root.getAttribute("class").contains("danger"); 
	}

    public String getText() {
        return root.getText();
    }

    public void waitUntilPresent() {
		waitGuiForElement(root, "Flash message should be visible.");
    }
}
