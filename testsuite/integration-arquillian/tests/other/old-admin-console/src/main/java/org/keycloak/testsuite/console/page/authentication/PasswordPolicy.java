package org.keycloak.testsuite.console.page.authentication;

import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
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
        if (value != null) {setPolicyValue(policy, value);}
        primaryButton.click();
        waitForPageToLoad();
    }


    public void addPolicy(Type policy, int value) {
        addPolicy(policy, String.valueOf(value));
    }

    public void addPolicy(Type policy) {
        addPolicy(policy, null);
    }

    public void removePolicy(Type policy) {
        getPolicyRow(policy).findElement(By.cssSelector("td.kc-action-cell")).click();
        if (primaryButton.isEnabled()) {
            primaryButton.click();
        }
        waitForPageToLoad();
    }

    public void editPolicy(Type policy, int value) {
        editPolicy(policy, String.valueOf(value));
    }

    public void editPolicy(Type policy, String value) {
        setPolicyValue(policy, value);
        if (primaryButton.isEnabled()) {
            primaryButton.click();
        }
        waitForPageToLoad();
    }

    private void setPolicyValue(Type policy, String value) {
        WebElement input = getPolicyRow(policy).findElement(By.tagName("input"));
        UIUtils.setTextInputValue(input, value);
    }

    private WebElement getPolicyRow(Type policy) {
        return table.findElement(By.xpath("//tr[td[text()='" + policy.getName() + "']]"));
    }

    public enum Type {

        HASH_ITERATIONS("Hashing Iterations"), LENGTH("Minimum Length"), DIGITS("Digits"), LOWER_CASE("Lowercase Characters"),
        UPPER_CASE("Uppercase Characters"), SPECIAL_CHARS("Special Characters"), NOT_USERNAME("Not Username"),
        REGEX_PATTERN("Regular Expression"), PASSWORD_HISTORY("Not Recently Used"),
        FORCE_EXPIRED_PASSWORD_CHANGE("Expire Password"), HASH_ALGORITHM("Hashing Algorithm");

        private String name;

        private Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
