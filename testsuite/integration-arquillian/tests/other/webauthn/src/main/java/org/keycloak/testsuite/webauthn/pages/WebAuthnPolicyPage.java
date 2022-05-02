/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.webauthn.pages;

import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.AuthenticatorAttachment;
import com.webauthn4j.data.UserVerificationRequirement;
import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.console.page.authentication.Authentication;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.console.page.idp.mappers.MultivaluedStringProperty;
import org.keycloak.testsuite.webauthn.utils.PropertyRequirement;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.function.Supplier;

import static org.keycloak.testsuite.util.UIUtils.getTextInputValue;
import static org.keycloak.testsuite.util.UIUtils.setTextInputValue;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * Helper class for WebAuthnPolicy Page
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class WebAuthnPolicyPage extends Authentication {

    @FindBy(id = "name")
    private WebElement rpEntityName;

    @FindBy(xpath = "//select[@id='sigalg']")
    private Select signatureAlgorithms;

    @FindBy(id = "rpid")
    private WebElement rpEntityId;

    @FindBy(id = "attpref")
    private Select attestationConveyancePreference;

    @FindBy(id = "authnatt")
    private Select authenticatorAttachment;

    @FindBy(id = "reqresident")
    private Select requireResidentKey;

    @FindBy(id = "usrverify")
    private Select userVerification;

    @FindBy(id = "timeout")
    private WebElement timeout;

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='avoidsame']]")
    private OnOffSwitch avoidSameAuthenticatorRegister;

    @Page
    private MultivaluedAcceptableAaguid acceptableAaguid;

    @FindBy(xpath = "//button[text()='Save']")
    private WebElement saveButton;

    @FindBy(xpath = "//button[text()='Cancel']")
    private WebElement cancelButton;

    @Override
    public String getUriFragment() {
        return getAuthenticationUriFragment() + "/webauthn-policy";
    }

    public String getAuthenticationUriFragment() {
        return super.getUriFragment();
    }

    /* Relaying Party Entity Name */

    public String getRpEntityName() {
        waitUntilElement(checkElement(() -> rpEntityName)).is().present();
        return getTextInputValue(rpEntityName);
    }

    public void setRpEntityName(String entityName) {
        waitUntilElement(checkElement(() -> rpEntityName)).is().clickable();
        setTextInputValue(rpEntityName, entityName);
    }

    /* Signature Algorithms */

    public Select getSignatureAlgorithms() {
        return checkElement(() -> signatureAlgorithms);
    }

    /* Relaying Party Entity ID */

    public String getRpEntityId() {
        waitUntilElement(checkElement(() -> rpEntityId)).is().present();
        return getTextInputValue(rpEntityId);
    }

    public void setRpEntityId(String id) {
        waitUntilElement(checkElement(() -> rpEntityId)).is().clickable();
        setTextInputValue(rpEntityId, id);
    }

    /* Attestation Conveyance Preference */

    public int getAttestationConveyancePreferenceItemsCount() {
        return checkElement(() -> attestationConveyancePreference.getOptions().size());
    }

    public AttestationConveyancePreference getAttestationConveyancePreference() {
        return getRequirementOrNull(() ->
                AttestationConveyancePreference.create(checkElement(() -> attestationConveyancePreference
                        .getFirstSelectedOption()
                        .getText()))
        );
    }

    public void setAttestationConveyancePreference(AttestationConveyancePreference attestation) {
        checkElement(() -> attestationConveyancePreference)
                .selectByValue(attestation.getValue());
    }

    /* Authenticator Attachment */

    public int getAuthenticatorAttachmentItemsCount() {
        return checkElement(() -> authenticatorAttachment.getOptions().size());
    }

    public AuthenticatorAttachment getAuthenticatorAttachment() {
        return getRequirementOrNull(() ->
                AuthenticatorAttachment.create(checkElement(() -> authenticatorAttachment
                        .getFirstSelectedOption()
                        .getText()))
        );
    }

    public void setAuthenticatorAttachment(AuthenticatorAttachment attachment) {
        checkElement(() -> authenticatorAttachment).selectByValue(attachment.getValue());
    }

    /* Require Resident Key */
    // If returns null, the requirement for resident key is not set up
    public PropertyRequirement requireResidentKey() {
        final int size = checkElement(() -> requireResidentKey).getAllSelectedOptions().size();
        if (size == 0) return null;

        final String value = requireResidentKey.getFirstSelectedOption().getText();
        return PropertyRequirement.fromValue(value);
    }

    // If parameter state is null, the requirement is considered as not set up
    public void requireResidentKey(PropertyRequirement requiresProperty) {
        if (requiresProperty == null) return;
        Select select = checkElement(() -> requireResidentKey);
        select.selectByVisibleText(requiresProperty.getValue());
    }

    /* User Verification Requirement */

    public int getUserVerificationItemsCount() {
        return checkElement(() -> userVerification).getOptions().size();
    }

    public UserVerificationRequirement getUserVerification() {
        return getRequirementOrNull(() ->
                UserVerificationRequirement.create(checkElement(() -> userVerification
                        .getFirstSelectedOption()
                        .getText()))
        );
    }

    public void setUserVerification(UserVerificationRequirement verification) {
        checkElement(() -> userVerification).selectByValue(verification.getValue());
    }

    /* Timeout */
    public int getTimeout() {
        final String value = getTextInputValue(checkElement(() -> timeout));
        return checkElement(() -> value == null || value.isEmpty() ? 0 : Integer.parseInt(value));
    }

    public void setTimeout(Integer time) {
        waitUntilElement(checkElement(() -> timeout)).is().clickable();
        setTextInputValue(timeout, time == null ? "0" : String.valueOf(time));
    }

    /* Avoid Same Authenticator Registration */
    public boolean avoidSameAuthenticatorRegistration() {
        return checkElement(() -> avoidSameAuthenticatorRegister.isOn());
    }

    public void avoidSameAuthenticatorRegister(boolean state) {
        if (avoidSameAuthenticatorRegistration() != state) {
            checkElement(() -> avoidSameAuthenticatorRegister).setOn(state);
        }
    }

    public MultivaluedAcceptableAaguid getAcceptableAaguid() {
        return acceptableAaguid;
    }

    /* Buttons */
    public void clickSaveButton() {
        waitUntilElement(checkElement(() -> saveButton)).is().clickable();
        saveButton.click();
        waitForPageToLoad();
    }

    public void clickCancelButton() {
        waitUntilElement(checkElement(() -> cancelButton)).is().clickable();
        cancelButton.click();
        waitForPageToLoad();
    }

    public boolean isSaveButtonEnabled() {
        waitUntilElement(checkElement(() -> saveButton)).is().present();
        return saveButton.isEnabled();
    }

    public boolean isCancelButtonEnabled() {
        waitUntilElement(checkElement(() -> cancelButton)).is().present();
        return cancelButton.isEnabled();
    }

    private <T> T checkElement(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (NoSuchElementException e) {
            throw new RuntimeException("Cannot find required element in WebAuthn Policy");
        } catch (NumberFormatException e) {
            throw new RuntimeException("Cannot convert element value to number in WebAuthn Policy");
        }
    }

    private <T> T getRequirementOrNull(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public class MultivaluedAcceptableAaguid extends MultivaluedStringProperty {

        @FindBy(className = "webauthn-acceptable-aaguid")
        private List<WebElement> aaguids;

        @FindBy(id = "newAcceptableAaguid")
        private WebElement newAaguid;

        @FindBy(xpath = "//button[@data-ng-click='deleteAcceptableAaguid($index)']")
        private List<WebElement> minusButtons;

        @FindBy(xpath = "//button[@data-ng-click='newAcceptableAaguid.length > 0 && addAcceptableAaguid()']")
        private WebElement plusButton;

        @Override
        public List<WebElement> getItems() {
            return checkElement(() -> aaguids);
        }

        @Override
        public void addItem(String item) {
            setTextInputValue(checkElement(() -> newAaguid), item);
            clickAddItem();
        }

        @Override
        protected List<WebElement> getMinusButtons() {
            return minusButtons;
        }

        @Override
        protected WebElement getPlusButton() {
            return plusButton;
        }
    }
}
