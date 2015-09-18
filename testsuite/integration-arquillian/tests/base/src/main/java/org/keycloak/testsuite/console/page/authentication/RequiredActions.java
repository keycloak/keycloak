package org.keycloak.testsuite.console.page.authentication;

import org.openqa.selenium.By;

/**
 * @author tkyjovsk
 * @author mhajas
 */
public class RequiredActions extends Authentication {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/required-actions";
    }

    public void clickTermsAndConditionEnabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()='Terms and Conditions']/..//input[@type='checkbox' and @ng-model='requiredAction.enabled']")).click();
    }

    public void clickTermsAndConditionDefaultAction() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()='Terms and Conditions']/..//input[@type='checkbox' and @ng-model='requiredAction.defaultAction']")).click();
    }

    public void clickVerifyEmailEnabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()='Verify Email']/..//input[@type='checkbox' and @ng-model='requiredAction.enabled']")).click();
    }

    public void clickVerifyEmailDefaultAction() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()='Verify Email']/..//input[@type='checkbox' and @ng-model='requiredAction.defaultAction']")).click();
    }

    public void clickUpdatePasswordEnabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()='Update Password']/..//input[@type='checkbox' and @ng-model='requiredAction.enabled']")).click();
    }

    public void clickUpdatePasswordDefaultAction() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()='Update Password']/..//input[@type='checkbox' and @ng-model='requiredAction.defaultAction']")).click();
    }

    public void clickConfigureTotpEnabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()='Configure Totp']/..//input[@type='checkbox' and @ng-model='requiredAction.enabled']")).click();
    }

    public void clickConfigureTotpDefaultAction() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()='Configure Totp']/..//input[@type='checkbox' and @ng-model='requiredAction.defaultAction']")).click();
    }

    public void clickUpdateProfileEnabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()='Update Profile']/..//input[@type='checkbox' and @ng-model='requiredAction.enabled']")).click();
    }

    public void clickUpdateProfileDefaultAction() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()='Update Profile']/..//input[@type='checkbox' and @ng-model='requiredAction.defaultAction']")).click();
    }
}
