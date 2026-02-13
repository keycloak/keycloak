package org.keycloak.testsuite.pages;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.keycloak.testsuite.util.UIUtils;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class OID4VCCredentialOfferPage extends LanguageComboboxAwarePage {

    @FindBy(id = "credential-offer-uri-link")
    private WebElement credentialOfferUri;

    @FindBy(id = "continueVCOffer")
    private WebElement continueButton;

    @FindBy(name = "cancel-aia")
    private WebElement cancelAIAButton;

    /**
     * @return full URL from the page. Usually something like "openid-credential-offer://?credential_offer_uri=https%3A%2F%2Flocalhost..."
     */
    public String getCredentialOffer() {
        return credentialOfferUri.getDomAttribute("href");
    }

    /**
     * @return Only credential-offer-uri (Something like "https://localhost...")
     */
    public String getCredentialOfferUri() {
        String fullOffer = getCredentialOffer();
        String[] splits = fullOffer.split("credential_offer_uri=");
        if (splits.length < 2) {
            return null;
        }
        String url = splits[1];
        return URLDecoder.decode(url, StandardCharsets.UTF_8);
    }

    public void clickContinueButton() {
        UIUtils.clickLink(continueButton);
    }

    public void cancel() {
        UIUtils.clickLink(cancelAIAButton);
    }

    public boolean isCurrent() {
        try {
            driver.findElement(By.id("kc-credential-offer-uri"));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public boolean isCancelDisplayed() {
        try {
            return cancelAIAButton.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }
}
