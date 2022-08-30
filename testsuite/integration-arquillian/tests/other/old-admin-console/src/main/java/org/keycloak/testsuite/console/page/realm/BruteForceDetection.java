package org.keycloak.testsuite.console.page.realm;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author mhajas
 */
public class BruteForceDetection extends SecurityDefenses {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/brute-force";
    }

    @Page
    private BruteForceDetectionForm form;

    public BruteForceDetectionForm form() {
        return form;
    }

    public class BruteForceDetectionForm extends Form {

        @FindByJQuery("div[class='onoffswitch']")
        private OnOffSwitch protectionEnabled;

        @FindBy(id = "failureFactor")
        private WebElement maxLoginFailures;

        @FindBy(id = "waitIncrement")
        private WebElement waitIncrementInput;

        @FindBy(name = "waitIncrementUnit")
        private Select waitIncrementSelect;

        @FindBy(id = "quickLoginCheckMilliSeconds")
        private WebElement quickLoginCheckInput;

        @FindBy(id = "minimumQuickLoginWait")
        private WebElement minQuickLoginWaitInput;

        @FindBy(name = "minimumQuickLoginWaitUnit")
        private Select minQuickLoginWaitSelect;

        @FindBy(id = "maxFailureWait")
        private WebElement maxWaitInput;

        @FindBy(name = "maxFailureWaitUnit")
        private Select maxWaitSelect;

        @FindBy(id = "maxDeltaTime")
        private WebElement failureResetTimeInput;

        @FindBy(name = "maxDeltaTimeUnit")
        private Select failureResetTimeSelect;

        public void setProtectionEnabled(boolean protectionEnabled) {
            this.protectionEnabled.setOn(protectionEnabled);
        }

        public void setMaxLoginFailures(String value) {
            UIUtils.setTextInputValue(maxLoginFailures, value);
        }

        public void setWaitIncrementInput(String value) {
            UIUtils.setTextInputValue(waitIncrementInput, value);
        }

        public void setWaitIncrementSelect(TimeSelectValues value) {
            waitIncrementSelect.selectByValue(value.getName());
        }

        public void setQuickLoginCheckInput(String value) {
            UIUtils.setTextInputValue(quickLoginCheckInput, value);
        }

        public void setMinQuickLoginWaitInput(String value) {
            UIUtils.setTextInputValue(minQuickLoginWaitInput, value);
        }

        public void setMinQuickLoginWaitSelect(TimeSelectValues value) {
            minQuickLoginWaitSelect.selectByValue(value.getName());
        }

        public void setMaxWaitInput(String value) {
            UIUtils.setTextInputValue(maxWaitInput, value);
        }

        public void setMaxWaitSelect(TimeSelectValues value) {
            maxWaitSelect.selectByValue(value.getName());
        }

        public void setFailureResetTimeInput(String value) {
            UIUtils.setTextInputValue(failureResetTimeInput, value);
        }

        public void setFailureResetTimeSelect(TimeSelectValues value) {
            failureResetTimeSelect.selectByValue(value.getName());
        }
    }

    public enum TimeSelectValues {

        SECONDS("Seconds"), MINUTES("Minutes"), HOURS("Hours"), DAYS("Days");

        private String name;

        private TimeSelectValues(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}