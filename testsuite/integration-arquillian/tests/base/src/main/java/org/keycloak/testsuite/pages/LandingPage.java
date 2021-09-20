package org.keycloak.testsuite.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Backup Code Authentication test
 *
 * @author <a href="mailto:vnukala@redhat.com">Venkata Nukala</a>
 */
public class LandingPage extends LanguageComboboxAwarePage {

    @FindBy(id = "landing-signingin")
    private WebElement signingInLink;

    @FindBy(id = "landing-device-activity")
    private WebElement deviceActivityLink;

    @FindBy(id = "landing-personal-info")
    private WebElement personalInfoLink;

    @FindBy(id = "landing-applications")
    private WebElement applicationsLink;

    public void clickSigningInLink() {
        driver.findElement(By.xpath("//a[contains(.,'Signing In')]")).click();
    }

    @Override
    public boolean isCurrent() {

        // Check the backup code text box and label available
        try {
            driver.findElement(By.id("landing-signingin"));
            driver.findElement(By.id("landing-device-activity"));
            driver.findElement(By.id("landing-personal-info"));
            driver.findElement(By.id("landing-applications"));
        } catch (NoSuchElementException nfe) {
            return false;
        }

        return true;
    }

    @Override
    public void open() throws Exception {
        throw new UnsupportedOperationException();
    }
}
