package org.keycloak.testsuite.pages;

import javax.ws.rs.core.UriBuilder;

import org.keycloak.services.Urls;
import org.keycloak.testsuite.Constants;
import org.openqa.selenium.By;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AccountFederatedIdentityPage extends AbstractAccountPage {

    public AccountFederatedIdentityPage() {};

    private String realmName = "test";

    public void open() {
        driver.navigate().to(getPath());
    }

    public void realm(String realmName) {
        this.realmName = realmName;
    }

    public String getPath() {
        return Urls.accountFederatedIdentityPage(UriBuilder.fromUri(Constants.AUTH_SERVER_ROOT).build(), realmName).toString();
    }

    @Override
    public boolean isCurrent() {
        return driver.getTitle().contains("Account Management") && driver.getPageSource().contains("Federated Identities");
    }

    public void clickAddProvider(String providerId) {
        driver.findElement(By.id("add-" + providerId)).click();
    }

    public void clickRemoveProvider(String providerId) {
        driver.findElement(By.id("remove-" + providerId)).click();
    }
}
