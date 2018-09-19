package org.keycloak.testsuite.console.page.realm;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.Map;

/**
 * Created by mhajas on 8/25/15.
 */
public class EmailSettings extends RealmSettings {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/smtp-settings";
    }

    @Page
    private EmailSettingsForm form;

    public EmailSettingsForm form() {
        return form;
    }

    public class EmailSettingsForm extends Form {
        @FindBy(id = "smtpHost")
        private WebElement hostInput;

        @FindBy(id = "smtpPort")
        private WebElement portInput;

        @FindBy(id = "smtpFrom")
        private WebElement fromInput;

        @FindByJQuery("div[class='onoffswitch']:eq(0)")
        private OnOffSwitch enableSSL;

        @FindByJQuery("div[class='onoffswitch']:eq(1)")
        private OnOffSwitch enableStartTLS;

        @FindByJQuery("div[class='onoffswitch']:eq(2)")
        private OnOffSwitch enableAuthentication;

        public void setEnableSSL(boolean sslEnabled) {
            enableSSL.setOn(sslEnabled);
        }

        public void setEnableStartTLS(boolean startTLS) {
            enableSSL.setOn(startTLS);
        }

        public void setEnableAuthentication(boolean authentication) {
            enableSSL.setOn(authentication);
        }

        public void setHostInput(String value) {
            UIUtils.setTextInputValue(hostInput, value);
        }

        public void setPortInput(String value) {
            UIUtils.setTextInputValue(portInput, value);
        }

        public void setFromInput(String value) {
            UIUtils.setTextInputValue(fromInput, value);
        }
        
        public void setSmtpServer(Map<String, String> smtpServer) {
            setFromInput(smtpServer.get("from"));
            setHostInput(smtpServer.get("host"));
            setPortInput(smtpServer.get("port"));
            
            save();
        }
    }
}
