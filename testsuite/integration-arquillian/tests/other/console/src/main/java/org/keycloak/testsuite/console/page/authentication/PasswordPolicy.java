package org.keycloak.testsuite.console.page.authentication;

import org.jboss.arquillian.graphene.findby.ByJQuery;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author Petr Mensik
 * @author tkyjovsk
 * @author mhajas
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class PasswordPolicy extends Authentication {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/password-policy";
    }

    @FindBy(tagName = "select")
    private Select addPolicySelect;

    @FindBy(tagName = "select")
    private WebElement addPolicySelectElement;

    @FindBy(tagName = "table")
    private WebElement table;

    public void addPolicy(Type policy, String value) {
        waitUntilElement(addPolicySelectElement).is().present();
        addPolicySelect.selectByVisibleText(policy.getName());
        setPolicyValue(policy, value);
        primaryButton.click();
    }


    public void addPolicy(Type policy, int value) {
        addPolicy(policy, String.valueOf(value));
    }

    public void addPolicy(Type policy) {
        addPolicySelect.selectByVisibleText(policy.getName());
        primaryButton.click();
    }

    public void removePolicy(Type policy) {
        getPolicyRow(policy).findElement(By.cssSelector("td.kc-action-cell")).click();
        primaryButton.click();
    }

    public void editPolicy(Type policy, int value) {
        editPolicy(policy, String.valueOf(value));
    }

    public void editPolicy(Type policy, String value) {
        setPolicyValue(policy, value);
        primaryButton.click();
    }

    private void setPolicyValue(Type policy, String value) {
        WebElement input = getPolicyRow(policy).findElement(By.tagName("input"));
        Form.setInputValue(input, value);
    }

    private WebElement getPolicyRow(Type policy) {
        return table.findElement(By.xpath("//tr[td[text()='" + policy.getName() + "']]"));
    }

    public enum Type {

        HASH_ITERATIONS("HashIterations"), LENGTH("Length"), DIGITS("Digits"), LOWER_CASE("LowerCase"),
        UPPER_CASE("UpperCase"), SPECIAL_CHARS("SpecialChars"), NOT_USERNAME("NotUsername"),
        REGEX_PATTERN("RegexPattern"), PASSWORD_HISTORY("PasswordHistory"),
        FORCE_EXPIRED_PASSWORD_CHANGE("ForceExpiredPasswordChange");

        private String name;

        private Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
