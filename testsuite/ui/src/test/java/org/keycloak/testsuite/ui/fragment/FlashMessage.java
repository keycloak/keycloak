package org.keycloak.testsuite.ui.fragment;

import static org.jboss.arquillian.graphene.Graphene.waitGui;
import org.jboss.arquillian.graphene.fragment.Root;

import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitGuiForElement;
import org.openqa.selenium.WebElement;

public class FlashMessage {

    @Root
    private WebElement root;

    public boolean isSuccess() {
		waitGui().until("Flash message should be success")
			.element(root)
			.attribute("class")
			.contains("success");
		return root.getAttribute("class").contains("success");
    }

    public boolean isError() {
		waitGui().until("Flash message should be error")
			.element(root)
			.attribute("class")
			.contains("error");
        return root.getAttribute("class").contains("error");
    }

    public boolean isDanger() {
		waitGui().until("Flash message should be danger")
			.element(root)
			.attribute("class")
			.contains("danger");
		return root.getAttribute("class").contains("danger"); 
	}

    public String getText() {
        return root.getText();
    }

    public void waitUntilPresent() {
		waitGuiForElement(root, "Flash message should be visible.");
    }
}
