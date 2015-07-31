package org.keycloak.testsuite.console.page.authentication;

import java.util.List;
import org.jboss.arquillian.graphene.findby.ByJQuery;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
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

    public void addPolicy(PasswordPolicy.Type policy, int value) {
        addPolicySelect.selectByVisibleText(policy.getName());
        setPolicyValue(policy, value);
        primaryButton.click();
    }

    public void removePolicy(PasswordPolicy.Type policy) {
        int policyInputLocation = findPolicy(policy);
        allRows.get(policyInputLocation).findElements(By.tagName("i")).get(0).click();
        primaryButton.click();
    }

    public void editPolicy(PasswordPolicy.Type policy, int value) {
        setPolicyValue(policy, value);
        primaryButton.click();
    }

    private void setPolicyValue(PasswordPolicy.Type policy, int value) {
        int policyInputLocation = findPolicy(policy);
        allRows.get(policyInputLocation).findElement(By.tagName("input")).sendKeys(String.valueOf(value));
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
        UPPER_CASE("Upper Case"), SPECIAL_CHARS("Special Chars");

        private String name;

        private Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
