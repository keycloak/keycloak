package org.keycloak.testsuite.console.page.realm;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author mhajas
 */
public class Headers extends SecurityDefenses {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/headers";
    }

    @Page
    private HeadersForm form;

    public HeadersForm form() {
        return form;
    }

    public class HeadersForm extends Form {

        @FindBy(id = "xFrameOptions")
        private WebElement xFrameOptions;

        @FindBy(id = "contentSecurityPolicy")
        private WebElement contentSecurityPolicy;

        public void setXFrameOptions(String value) {
            UIUtils.setTextInputValue(xFrameOptions, value);
        }

        public void setContentSecurityPolicy(String value) {
            UIUtils.setTextInputValue(contentSecurityPolicy, value);
        }
    }
}
