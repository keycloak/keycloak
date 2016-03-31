package org.keycloak.testsuite.console.page.clients;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.page.Form;
import static org.keycloak.testsuite.page.Form.getInputValue;
import static org.keycloak.testsuite.util.WaitUtils.*;
import org.keycloak.testsuite.util.Timer;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

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
        return getInputValue(clientIdInput);
    }

    public void setClientId(String clientId) {
        setInputValue(clientIdInput, clientId);
    }

    public String getProtocol() {
        waitUntilElement(protocolSelect.getFirstSelectedOption()).is().present();
        return protocolSelect.getFirstSelectedOption().getText();
    }

    public void setProtocol(String protocol) {
        Timer.DEFAULT.reset();
        protocolSelect.selectByVisibleText(protocol);
        Timer.DEFAULT.reset("clientSettings.setProtocol()");
    }
}