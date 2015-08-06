package org.keycloak.testsuite.console.page.clients;

import java.util.List;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;
import static org.keycloak.testsuite.util.SeleniumUtils.pause;
import static org.keycloak.testsuite.util.SeleniumUtils.waitAjaxForElement;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class ClientForm extends Form {

    @FindBy(id = "clientId")
    private WebElement clientIdInput;

    @FindBy(id = "name")
    private WebElement nameInput;

    @FindBy(xpath = "//div[@class='onoffswitch' and ./input[@id='enabled']]")
    private OnOffSwitch enabledSwitch;

    @FindBy(id = "accessType")
    private WebElement accessTypeDropDownMenu;

    @FindBy(id = "newRedirectUri")
    private WebElement redirectUriInput;

    public String getClientId() {
        return clientIdInput.getText();
    }

    public void setClientId(String clientId) {
        clientIdInput.clear();
        if (clientId != null) {
            clientIdInput.sendKeys(clientId);
        }
    }

    public void setName(String name) {
        nameInput.clear();
        if (name != null) {
            nameInput.sendKeys(name);
        }
    }

    public void setEnabled(boolean enabled) {
        enabledSwitch.setOn(enabled);
    }

    public void setAccessType(ClientRepresentation client) {
        if (client.isDirectGrantsOnly()) { // TODO verify this one
            accessTypeDropDownMenu.sendKeys("confidential");
        }
        if (client.isBearerOnly()) {
            accessTypeDropDownMenu.sendKeys("bearer-only");
        }
        if (client.isPublicClient()) {
            accessTypeDropDownMenu.sendKeys("public");
        }
    }

    public void setRedirectUris(List<String> redirectUris) {
        redirectUriInput.clear();
        if (redirectUris != null) {
            for (String redirectUri : redirectUris) {
                addRedirectUri(redirectUri);
                pause(100);
            }
        }
    }

    public void addRedirectUri(String redirectUri) {
        redirectUriInput.sendKeys(redirectUri);
    }

    public void setValues(ClientRepresentation client) {
        waitAjaxForElement(clientIdInput);

        setClientId(client.getClientId());
        setName(client.getName());
        setEnabled(client.isEnabled());
        setAccessType(client);
        setRedirectUris(client.getRedirectUris());
    }

}
