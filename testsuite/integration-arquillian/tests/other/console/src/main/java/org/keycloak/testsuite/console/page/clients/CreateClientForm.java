package org.keycloak.testsuite.console.page.clients;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.Timer;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.UIUtils.getTextInputValue;
import static org.keycloak.testsuite.util.WaitUtils.*;

/**
 *
 * @author tkyjovsk
 */
public class CreateClientForm extends Form {

    @FindBy(id = "clientId")
    private WebElement clientIdInput;

    @FindBy(id = "protocol")
    private Select protocolSelect;
    
    public void setValues(ClientRepresentation client) {
        waitUntilElement(clientIdInput).is().present();

        setClientId(client.getClientId());
        setProtocol(client.getProtocol());
    }

    public String getClientId() {
        return getTextInputValue(clientIdInput);
    }

    public void setClientId(String clientId) {
        UIUtils.setTextInputValue(clientIdInput, clientId);
    }

    public String getProtocol() {
        waitUntilElement(protocolSelect.getFirstSelectedOption()).is().present();
        return getTextFromElement(protocolSelect.getFirstSelectedOption());
    }

    public void setProtocol(String protocol) {
        Timer.DEFAULT.reset();
        protocolSelect.selectByVisibleText(protocol);
        Timer.DEFAULT.reset("clientSettings.setProtocol()");
    }
}