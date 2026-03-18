package org.keycloak.testframework.ui.page;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class OID4VCCredentialOfferPage extends AbstractLoginPage {

    @FindBy(id = "credential-offer-uri-link")
    private WebElement credentialOfferUri;

    @FindBy(id = "continue-vc-offer")
    private WebElement continueButton;

    @FindBy(name = "cancel-aia")
    private WebElement cancelAIAButton;

    public OID4VCCredentialOfferPage(ManagedWebDriver driver) {
        super(driver);
    }

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
        continueButton.click();
    }


    public void cancel() {
        cancelAIAButton.click();
    }

    @Override
    public String getExpectedPageId() {
        return "login-oid4vc-credential-offer";
    }
}
