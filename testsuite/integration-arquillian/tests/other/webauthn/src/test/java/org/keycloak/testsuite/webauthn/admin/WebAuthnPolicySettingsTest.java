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

package org.keycloak.testsuite.webauthn.admin;

import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.AuthenticatorAttachment;
import com.webauthn4j.data.UserVerificationRequirement;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.page.AbstractPatternFlyAlert;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.UIUtils;
import org.keycloak.testsuite.webauthn.pages.WebAuthnPolicyPage;
import org.keycloak.testsuite.webauthn.pages.WebAuthnPolicyPasswordlessPage;
import org.keycloak.testsuite.webauthn.updaters.AbstractWebAuthnRealmUpdater;
import org.keycloak.testsuite.webauthn.updaters.PasswordLessRealmAttributeUpdater;
import org.keycloak.testsuite.webauthn.updaters.WebAuthnRealmAttributeUpdater;
import org.keycloak.testsuite.webauthn.utils.PropertyRequirement;
import org.keycloak.testsuite.webauthn.utils.WebAuthnRealmData;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.webauthn4j.data.AttestationConveyancePreference.INDIRECT;
import static com.webauthn4j.data.AuthenticatorAttachment.CROSS_PLATFORM;
import static com.webauthn4j.data.UserVerificationRequirement.PREFERRED;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.keycloak.models.Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.keycloak.testsuite.webauthn.utils.PropertyRequirement.NO;
import static org.keycloak.testsuite.webauthn.utils.PropertyRequirement.YES;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class WebAuthnPolicySettingsTest extends AbstractConsoleTest {

    protected static final String ALL_ZERO_AAGUID = "00000000-0000-0000-0000-000000000000";
    protected static final String ALL_ONE_AAGUID = "11111111-1111-1111-1111-111111111111";

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    WebAuthnPolicyPage webAuthnPolicyPage;

    @Page
    WebAuthnPolicyPasswordlessPage webAuthnPolicyPasswordlessPage;

    @Before
    public void navigateToPolicy() {
        driver.manage().window().maximize();
        getPolicyPage().navigateTo();
        waitForPageToLoad();
        getPolicyPage().assertCurrent();
    }

    protected boolean isPasswordless() {
        return false;
    }

    protected WebAuthnPolicyPage getPolicyPage() {
        return isPasswordless() ? webAuthnPolicyPasswordlessPage : webAuthnPolicyPage;
    }

    protected AbstractWebAuthnRealmUpdater<?> getWebAuthnRealmUpdater() {
        return isPasswordless() ? new PasswordLessRealmAttributeUpdater(testRealmResource()) : new WebAuthnRealmAttributeUpdater(testRealmResource());
    }

    protected AbstractWebAuthnRealmUpdater<?> updateWebAuthnPolicy(
            String rpName,
            List<String> algorithms,
            String attestationPreference,
            String authenticatorAttachment,
            String requireResidentKey,
            String rpId,
            String userVerification,
            List<String> acceptableAaguids) {

        AbstractWebAuthnRealmUpdater<?> updater = getWebAuthnRealmUpdater().setWebAuthnPolicyRpEntityName(rpName);

        checkAndSet(algorithms, updater::setWebAuthnPolicySignatureAlgorithms);
        checkAndSet(attestationPreference, updater::setWebAuthnPolicyAttestationConveyancePreference);
        checkAndSet(authenticatorAttachment, updater::setWebAuthnPolicyAuthenticatorAttachment);
        checkAndSet(requireResidentKey, updater::setWebAuthnPolicyRequireResidentKey);
        checkAndSet(rpId, updater::setWebAuthnPolicyRpId);
        checkAndSet(userVerification, updater::setWebAuthnPolicyUserVerificationRequirement);
        checkAndSet(acceptableAaguids, updater::setWebAuthnPolicyAcceptableAaguids);

        return updater.update();
    }

    private <T> void checkAndSet(T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    @Test
    public void policySettingsWithExternalProperties() throws IOException {
        try (RealmAttributeUpdater rau = updateWebAuthnPolicy(
                "rpName",
                Collections.singletonList("ES256"),
                INDIRECT.getValue(),
                CROSS_PLATFORM.getValue(),
                "No",
                null,
                PREFERRED.getValue(),
                Collections.singletonList(ALL_ZERO_AAGUID))
        ) {

            WebAuthnRealmData realm = new WebAuthnRealmData(testRealmResource().toRepresentation(), isPasswordless());
            assertThat(realm, notNullValue());

            assertThat(realm.getSignatureAlgorithms(), hasItems("ES256"));
            assertThat(realm.getAttestationConveyancePreference(), is(INDIRECT.getValue()));
            assertThat(realm.getAuthenticatorAttachment(), is(CROSS_PLATFORM.getValue()));
            assertThat(realm.getRequireResidentKey(), is("No"));
            assertThat(realm.getRpId(), is(""));
            assertThat(realm.getUserVerificationRequirement(), is(PREFERRED.getValue()));
            assertThat(realm.getAcceptableAaguids(), hasItems(ALL_ZERO_AAGUID));
        }
    }

    @Test
    public void rpEntityValues() {
        String rpEntityName = getPolicyPage().getRpEntityName();
        assertThat(rpEntityName, notNullValue());
        assertThat(rpEntityName, is(Constants.DEFAULT_WEBAUTHN_POLICY_RP_ENTITY_NAME));

        getPolicyPage().setRpEntityName("newEntityName");
        getPolicyPage().clickSaveButton();
        AbstractPatternFlyAlert.waitUntilHidden();

        rpEntityName = getPolicyPage().getRpEntityName();
        assertThat(rpEntityName, notNullValue());
        assertThat(rpEntityName, is("newEntityName"));

        getPolicyPage().setRpEntityName("");
        getPolicyPage().clickSaveButton();
        AbstractPatternFlyAlert.waitUntilHidden();

        rpEntityName = getPolicyPage().getRpEntityName();
        assertThat(rpEntityName, notNullValue());
        assertThat(rpEntityName, is(Constants.DEFAULT_WEBAUTHN_POLICY_RP_ENTITY_NAME));

        String rpEntityId = getPolicyPage().getRpEntityId();
        assertThat(rpEntityId, notNullValue());
        assertThat(rpEntityId, is(""));

        getPolicyPage().setRpEntityId("rpId123");
        getPolicyPage().clickSaveButton();
        AbstractPatternFlyAlert.waitUntilHidden();

        rpEntityId = getPolicyPage().getRpEntityId();
        assertThat(rpEntityId, notNullValue());
        assertThat(rpEntityId, is("rpId123"));

        final WebAuthnRealmData realm = new WebAuthnRealmData(testRealmResource().toRepresentation(), isPasswordless());
        assertThat(realm, notNullValue());

        assertThat(realm.getRpEntityName(), is(Constants.DEFAULT_WEBAUTHN_POLICY_RP_ENTITY_NAME));
        assertThat(realm.getRpId(), is("rpId123"));
    }

    @Test
    public void wrongSignatureAlgorithm() throws IOException {
        try (Closeable c = getWebAuthnRealmUpdater()
                .setWebAuthnPolicySignatureAlgorithms(Collections.singletonList("something-bad"))
                .update()) {

            RealmRepresentation realm = testRealmResource().toRepresentation();
            assertThat(realm, notNullValue());

            final List<String> signatureAlgorithms = realm.getWebAuthnPolicySignatureAlgorithms();
            assertThat(signatureAlgorithms, notNullValue());
            assertThat(signatureAlgorithms.size(), is(1));

            getPolicyPage().navigateTo();
            waitForPageToLoad();

            Select selectedAlg = getPolicyPage().getSignatureAlgorithms();
            assertThat(selectedAlg, notNullValue());

            try {
                // should throw an exception
                selectedAlg.getFirstSelectedOption();
            } catch (NoSuchElementException e) {
                assertThat(e.getMessage(), containsString("No options are selected"));
            }
        }
    }

    @Test
    public void signatureAlgorithms() {
        getPolicyPage().assertCurrent();

        final Select algorithms = getPolicyPage().getSignatureAlgorithms();
        assertThat(algorithms, notNullValue());

        algorithms.selectByValue("ES256");
        algorithms.selectByValue("ES384");
        algorithms.selectByValue("RS1");

        final List<String> selectedAlgs = algorithms.getAllSelectedOptions()
                .stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());

        assertThat(selectedAlgs, notNullValue());
        assertThat(selectedAlgs, hasSize(3));

        try {
            algorithms.selectByValue("something-bad");
        } catch (NoSuchElementException e) {
            assertThat(e.getMessage(), containsString("Cannot locate option with value: something-bad"));
        }

        assertThat(getPolicyPage().isSaveButtonEnabled(), is(true));
        assertThat(getPolicyPage().isCancelButtonEnabled(), is(true));

        getPolicyPage().clickSaveButton();

        assertThat(getPolicyPage().isSaveButtonEnabled(), is(false));
        assertThat(getPolicyPage().isCancelButtonEnabled(), is(false));

        final WebAuthnRealmData realm = new WebAuthnRealmData(testRealmResource().toRepresentation(), isPasswordless());
        assertThat(realm, notNullValue());

        final List<String> realmSignatureAlgs = realm.getSignatureAlgorithms();
        assertThat(realmSignatureAlgs, notNullValue());
        assertThat(realmSignatureAlgs, hasSize(3));
        assertThat(realmSignatureAlgs, contains("ES256", "ES384", "RS1"));
    }

    @Test
    public void attestationConveyancePreference() {
        // default not specified
        AttestationConveyancePreference attestation = getPolicyPage().getAttestationConveyancePreference();
        assertThat(attestation, nullValue());

        // Direct
        getPolicyPage().setAttestationConveyancePreference(AttestationConveyancePreference.DIRECT);
        getPolicyPage().clickSaveButton();

        attestation = getPolicyPage().getAttestationConveyancePreference();
        assertThat(attestation, notNullValue());
        assertThat(attestation, is(AttestationConveyancePreference.DIRECT));

        // Indirect
        getPolicyPage().setAttestationConveyancePreference(AttestationConveyancePreference.INDIRECT);
        getPolicyPage().clickSaveButton();

        attestation = getPolicyPage().getAttestationConveyancePreference();
        assertThat(attestation, notNullValue());
        assertThat(attestation, is(AttestationConveyancePreference.INDIRECT));

        // None
        getPolicyPage().setAttestationConveyancePreference(AttestationConveyancePreference.NONE);
        getPolicyPage().clickSaveButton();

        attestation = getPolicyPage().getAttestationConveyancePreference();
        assertThat(attestation, notNullValue());
        assertThat(attestation, is(AttestationConveyancePreference.NONE));

        try {
            getPolicyPage().setAttestationConveyancePreference(AttestationConveyancePreference.ENTERPRISE);
            Assert.fail("We don't support 'Enterprise' mode at this moment");
        } catch (NoSuchElementException e) {
            // Expected - NOP
        }

        assertDataAfterModification(AttestationConveyancePreference.NONE.getValue(),
                DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED,
                WebAuthnRealmData::getAttestationConveyancePreference,
                (builder) -> builder.attestationConveyancePreference(null)
        );
    }

    @Test
    public void authenticatorAttachment() {
        AuthenticatorAttachment attachment = getPolicyPage().getAuthenticatorAttachment();
        assertThat(attachment, nullValue());

        // Cross-platform
        getPolicyPage().setAuthenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM);
        getPolicyPage().clickSaveButton();

        attachment = getPolicyPage().getAuthenticatorAttachment();
        assertThat(attachment, notNullValue());
        assertThat(attachment, is(AuthenticatorAttachment.CROSS_PLATFORM));

        // Platform
        getPolicyPage().setAuthenticatorAttachment(AuthenticatorAttachment.PLATFORM);
        getPolicyPage().clickSaveButton();

        attachment = getPolicyPage().getAuthenticatorAttachment();
        assertThat(attachment, notNullValue());
        assertThat(attachment, is(AuthenticatorAttachment.PLATFORM));

        assertDataAfterModification(AuthenticatorAttachment.PLATFORM.getValue(),
                DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED,
                WebAuthnRealmData::getAuthenticatorAttachment,
                (builder) -> builder.authenticatorAttachment(null)
        );
    }

    @Test
    public void residentKey() {
        PropertyRequirement requireResidentKey = getPolicyPage().requireResidentKey();
        assertThat(requireResidentKey, notNullValue());
        assertThat(requireResidentKey, is(PropertyRequirement.NOT_SPECIFIED));

        getPolicyPage().requireResidentKey(YES);
        getPolicyPage().clickSaveButton();

        // Yes
        requireResidentKey = getPolicyPage().requireResidentKey();
        assertThat(requireResidentKey, notNullValue());
        assertThat(requireResidentKey, is(YES));

        getPolicyPage().requireResidentKey(NO);
        getPolicyPage().clickSaveButton();

        // Null
        getPolicyPage().requireResidentKey(null);
        assertThat(getPolicyPage().isSaveButtonEnabled(), is(false));

        // Not specified
        getPolicyPage().requireResidentKey(PropertyRequirement.NOT_SPECIFIED);
        assertThat(getPolicyPage().isSaveButtonEnabled(), is(true));
        getPolicyPage().clickSaveButton();

        // No
        getPolicyPage().requireResidentKey(NO);
        getPolicyPage().clickSaveButton();

        requireResidentKey = getPolicyPage().requireResidentKey();
        assertThat(requireResidentKey, notNullValue());
        assertThat(requireResidentKey, is(NO));

        assertDataAfterModification(NO.getValue(),
                DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED,
                WebAuthnRealmData::getRequireResidentKey,
                (builder) -> builder.requireResidentKey(null)
        );
    }

    @Test
    public void userVerification() {
        UserVerificationRequirement userVerification = getPolicyPage().getUserVerification();
        assertThat(userVerification, nullValue());

        // Preferred
        getPolicyPage().setUserVerification(UserVerificationRequirement.PREFERRED);
        getPolicyPage().clickSaveButton();

        userVerification = getPolicyPage().getUserVerification();
        assertThat(userVerification, notNullValue());
        assertThat(userVerification, is(UserVerificationRequirement.PREFERRED));

        // Required
        getPolicyPage().setUserVerification(UserVerificationRequirement.REQUIRED);
        getPolicyPage().clickSaveButton();

        userVerification = getPolicyPage().getUserVerification();
        assertThat(userVerification, notNullValue());
        assertThat(userVerification, is(UserVerificationRequirement.REQUIRED));

        // Discouraged
        getPolicyPage().setUserVerification(UserVerificationRequirement.DISCOURAGED);
        getPolicyPage().clickSaveButton();

        userVerification = getPolicyPage().getUserVerification();
        assertThat(userVerification, notNullValue());
        assertThat(userVerification, is(UserVerificationRequirement.DISCOURAGED));

        assertDataAfterModification(UserVerificationRequirement.DISCOURAGED.getValue(),
                DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED,
                WebAuthnRealmData::getUserVerificationRequirement,
                (builder) -> builder.userVerificationRequirement(null)
        );
    }

    @Test
    public void timeout() {
        int timeout = getPolicyPage().getTimeout();
        assertThat(timeout, is(0));

        getPolicyPage().setTimeout(10);
        getPolicyPage().clickSaveButton();

        timeout = getPolicyPage().getTimeout();
        assertThat(timeout, is(10));

        getPolicyPage().setTimeout(-10);
        getPolicyPage().clickSaveButton();
        assertAlertDanger();

        timeout = getPolicyPage().getTimeout();
        assertThat(timeout, is(-10));

        getPolicyPage().navigateTo();
        waitForPageToLoad();

        timeout = getPolicyPage().getTimeout();
        assertThat(timeout, is(10));

        getPolicyPage().setTimeout(1000000);
        getPolicyPage().clickSaveButton();
        assertAlertDanger();

        getPolicyPage().setTimeout(500);
        getPolicyPage().clickSaveButton();

        timeout = getPolicyPage().getTimeout();
        assertThat(timeout, is(500));

        final WebAuthnRealmData realm = new WebAuthnRealmData(testRealmResource().toRepresentation(), isPasswordless());
        assertThat(realm, notNullValue());
        assertThat(realm.getCreateTimeout(), is(500));
    }

    @Test
    public void avoidSameAuthenticatorRegistration() {
        boolean avoidSameAuthenticatorRegistration = getPolicyPage().avoidSameAuthenticatorRegistration();
        assertThat(avoidSameAuthenticatorRegistration, is(false));

        getPolicyPage().avoidSameAuthenticatorRegister(true);
        assertThat(getPolicyPage().isSaveButtonEnabled(), is(true));
        getPolicyPage().clickSaveButton();

        avoidSameAuthenticatorRegistration = getPolicyPage().avoidSameAuthenticatorRegistration();
        assertThat(avoidSameAuthenticatorRegistration, is(true));

        getPolicyPage().avoidSameAuthenticatorRegister(false);
        getPolicyPage().clickSaveButton();

        avoidSameAuthenticatorRegistration = getPolicyPage().avoidSameAuthenticatorRegistration();
        assertThat(avoidSameAuthenticatorRegistration, is(false));

        final WebAuthnRealmData realm = new WebAuthnRealmData(testRealmResource().toRepresentation(), isPasswordless());
        assertThat(realm, notNullValue());
        assertThat(realm.isAvoidSameAuthenticatorRegister(), is(false));
    }

    @Test
    public void acceptableAaguid() {
        WebAuthnPolicyPage.MultivaluedAcceptableAaguid acceptableAaguid = getPolicyPage().getAcceptableAaguid();
        assertThat(acceptableAaguid, notNullValue());

        List<String> items = getAcceptableAaguid(getPolicyPage().getAcceptableAaguid());
        assertThat(items, notNullValue());

        acceptableAaguid.addItem(ALL_ONE_AAGUID);
        getPolicyPage().clickSaveButton();

        items = getAcceptableAaguid(getPolicyPage().getAcceptableAaguid());

        assertThat(items, notNullValue());
        assertThat(items.isEmpty(), is(false));
        assertThat(items.contains(ALL_ONE_AAGUID), is(true));

        final String YUBIKEY_5_AAGUID = "cb69481e-8ff7-4039-93ec-0a2729a154a8";
        final String YUBICO_AAGUID = "f8a011f3-8c0a-4d15-8006-17111f9edc7d";

        acceptableAaguid.addItem(YUBIKEY_5_AAGUID);
        acceptableAaguid.addItem(YUBICO_AAGUID);
        items = getAcceptableAaguid(getPolicyPage().getAcceptableAaguid());

        assertThat(items, notNullValue());
        assertThat(items, hasSize(3));

        getPolicyPage().clickSaveButton();
        acceptableAaguid.removeItem(0);
        items = getAcceptableAaguid(getPolicyPage().getAcceptableAaguid());

        assertThat(items, notNullValue());
        assertThat(items, hasSize(2));
        assertThat(items.contains(YUBICO_AAGUID), is(true));
        assertThat(items.contains(YUBIKEY_5_AAGUID), is(true));
        assertThat(items.contains(ALL_ONE_AAGUID), is(false));

        assertThat(getPolicyPage().isSaveButtonEnabled(), is(true));
        getPolicyPage().clickSaveButton();
        pause(100);

        WebAuthnRealmData realm = new WebAuthnRealmData(testRealmResource().toRepresentation(), isPasswordless());
        assertThat(realm, notNullValue());
        assertThat(realm.getAcceptableAaguids(), is(getAcceptableAaguid(getPolicyPage().getAcceptableAaguid())));
    }

    protected List<String> getAcceptableAaguid(WebAuthnPolicyPage.MultivaluedAcceptableAaguid acceptableAaguid) {
        return acceptableAaguid.getItems()
                .stream()
                .map(UIUtils::getTextInputValue)
                .collect(Collectors.toList());
    }

    /**
     * Assert WebAuthn Realm data before and after modification
     *
     * @param actualValue     actual value before modification
     * @param expectedValue   expected value after modification
     * @param getCurrentValue get updated value
     * @param setData         exact approach, how to change the realm data
     */
    private <T> void assertDataAfterModification(T actualValue, T expectedValue,
                                                 Function<WebAuthnRealmData, T> getCurrentValue,
                                                 Consumer<WebAuthnRealmData.Builder> setData) {

        WebAuthnRealmData realm = new WebAuthnRealmData(testRealmResource().toRepresentation(), isPasswordless());
        assertThat(realm, notNullValue());

        assertThat(getCurrentValue.apply(realm), is(actualValue));

        WebAuthnRealmData.Builder builder = realm.builder();
        assertThat(builder, notNullValue());
        setData.accept(builder);

        final RealmRepresentation newRealm = builder.build();
        assertThat(newRealm, notNullValue());

        testRealmResource().update(newRealm);

        realm = new WebAuthnRealmData(testRealmResource().toRepresentation(), isPasswordless());
        assertThat(realm, notNullValue());

        assertThat(getCurrentValue.apply(realm), is(expectedValue));
    }
}
