package org.keycloak.testsuite.console.page.authentication;

import java.util.List;

import org.jboss.arquillian.graphene.findby.ByJQuery;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author Petr Mensik
 * @author tkyjovsk
 * @author mhajas
 */
public class PasswordPolicy extends Authentication {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/password-policy";
    }

    @FindBy(tagName = "select")
    private Select addPolicySelect;

    @FindBy(css = "tr.ng-scope")
    private List<WebElement> allRows;

    public void addPolicy(PasswordPolicy.Type policy, String value) {
        addPolicySelect.selectByVisibleText(policy.getName());
        setPolicyValue(policy, value);
        primaryButton.click();
    }


    public void addPolicy(PasswordPolicy.Type policy, int value) {
        addPolicy(policy, String.valueOf(value));
    }

    public void addPolicy(PasswordPolicy.Type policy) {
        addPolicySelect.selectByVisibleText(policy.getName());
        primaryButton.click();
    }

    public void removePolicy(PasswordPolicy.Type policy) {
        int policyInputLocation = findPolicy(policy);
        allRows.get(policyInputLocation).findElements(By.tagName("button")).get(0).click();
        primaryButton.click();
    }

    public void editPolicy(PasswordPolicy.Type policy, int value) {
        editPolicy(policy, String.valueOf(value));
    }

    public void editPolicy(PasswordPolicy.Type policy, String value) {
        setPolicyValue(policy, value);
        primaryButton.click();
    }

    private void setPolicyValue(PasswordPolicy.Type policy, String value) {
        int policyInputLocation = findPolicy(policy);
        WebElement input = allRows.get(policyInputLocation).findElement(By.tagName("input"));
        input.clear();
        input.sendKeys(value);
    }

    private int findPolicy(PasswordPolicy.Type policy) {
        for (int i = 0; i < allRows.size(); i++) {
            String policyName = allRows.get(i).findElement(ByJQuery.selector("td:eq(0)")).getText();
            if (policyName.equals(policy.getName())) {
                return i;
            }
        }
        return 0;
    }

    public enum Type {

        HASH_ITERATIONS("Hash Iterations"), LENGTH("Length"), DIGITS("Digits"), LOWER_CASE("Lower Case"),
        UPPER_CASE("Upper Case"), SPECIAL_CHARS("Special Chars"), NOT_USERNAME("Not Username"),
        REGEX_PATTERNS("Regex Patterns"), PASSWORD_HISTORY("Password History"),
        FORCE_EXPIRED_PASSWORD_CHANGE("Force Expired Password Change");

        private String name;

        private Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
