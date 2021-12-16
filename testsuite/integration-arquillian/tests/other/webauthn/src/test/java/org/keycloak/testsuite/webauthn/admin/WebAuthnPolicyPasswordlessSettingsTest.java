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
import org.junit.Test;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.webauthn.pages.WebAuthnPolicyPage;
import org.keycloak.testsuite.webauthn.pages.WebAuthnPolicyPasswordlessPage;
import org.keycloak.testsuite.webauthn.updaters.AbstractWebAuthnRealmUpdater;
import org.keycloak.testsuite.webauthn.updaters.PasswordLessRealmAttributeUpdater;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.webauthn4j.data.AttestationConveyancePreference.DIRECT;
import static com.webauthn4j.data.AuthenticatorAttachment.PLATFORM;
import static com.webauthn4j.data.UserVerificationRequirement.PREFERRED;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.keycloak.models.Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class WebAuthnPolicyPasswordlessSettingsTest extends AbstractWebAuthnPolicySettingsTest {

    @Page
    WebAuthnPolicyPasswordlessPage webAuthnPolicyPasswordlessPage;

    @Override
    protected WebAuthnPolicyPage getPolicyPage() {
        return webAuthnPolicyPasswordlessPage;
    }

    @Override
    protected AbstractWebAuthnRealmUpdater getWebAuthnRealmUpdater() {
        return new PasswordLessRealmAttributeUpdater(testRealmResource());
    }

    @Test
    public void policySettingsWithExternalProperties() throws IOException {
        try (RealmAttributeUpdater rau = updateWebAuthnPolicy(
                "rpNamePasswordless",
                Collections.singletonList("RS256"),
                DIRECT.getValue(),
                PLATFORM.getValue(),
                "Yes",
                "1234",
                PREFERRED.getValue(),
                Collections.singletonList(ALL_ZERO_AAGUID))
        ) {
            RealmRepresentation realm = testRealmResource().toRepresentation();
            assertThat(realm, notNullValue());

            assertThat(realm.getWebAuthnPolicyPasswordlessSignatureAlgorithms(), hasItems("RS256"));
            assertThat(realm.getWebAuthnPolicyPasswordlessAttestationConveyancePreference(), is(DIRECT.getValue()));
            assertThat(realm.getWebAuthnPolicyPasswordlessAuthenticatorAttachment(), is(PLATFORM.getValue()));
            assertThat(realm.getWebAuthnPolicyPasswordlessRequireResidentKey(), is("Yes"));
            assertThat(realm.getWebAuthnPolicyPasswordlessRpId(), is("1234"));
            assertThat(realm.getWebAuthnPolicyPasswordlessUserVerificationRequirement(), is(PREFERRED.getValue()));
            assertThat(realm.getWebAuthnPolicyPasswordlessAcceptableAaguids(), hasItems(ALL_ZERO_AAGUID));
        }
    }

    @Test
    public void wrongSignatureAlgorithm() throws IOException {
        checkWrongSignatureAlgorithm();
    }

    @Test
    public void algorithmsValuesSetUpInAdminConsole() {
        checkSignatureAlgorithms();

        final RealmRepresentation realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        final List<String> realmSignatureAlgs = realm.getWebAuthnPolicyPasswordlessSignatureAlgorithms();
        assertThat(realmSignatureAlgs, notNullValue());
        assertThat(realmSignatureAlgs, hasSize(3));
        assertThat(realmSignatureAlgs, contains("ES256", "ES384", "RS1"));
    }

    @Test
    public void rpValuesSetUpInAdminConsole() {
        checkRpEntityValues();

        final RealmRepresentation realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyPasswordlessRpEntityName(), is(Constants.DEFAULT_WEBAUTHN_POLICY_RP_ENTITY_NAME));
        assertThat(realm.getWebAuthnPolicyPasswordlessRpId(), is("rpId123"));
    }

    @Test
    public void attestationConveyancePreferenceSettings() {
        checkAttestationConveyancePreference();

        RealmRepresentation realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyPasswordlessAttestationConveyancePreference(), is(AttestationConveyancePreference.NONE.getValue()));

        realm.setWebAuthnPolicyPasswordlessAttestationConveyancePreference(null);
        testRealmResource().update(realm);

        realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyPasswordlessAttestationConveyancePreference(), is(DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED));
    }

    @Test
    public void authenticatorAttachmentSettings() {
        checkAuthenticatorAttachment();

        RealmRepresentation realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyPasswordlessAuthenticatorAttachment(), is(AuthenticatorAttachment.PLATFORM.getValue()));

        realm.setWebAuthnPolicyPasswordlessAuthenticatorAttachment(null);
        testRealmResource().update(realm);

        realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyPasswordlessAuthenticatorAttachment(), is(DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED));
    }

    @Test
    public void requireResidentKeySettings() {
        checkResidentKey();

        RealmRepresentation realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyPasswordlessRequireResidentKey(), is("No"));

        realm.setWebAuthnPolicyPasswordlessRequireResidentKey(null);
        testRealmResource().update(realm);

        realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyPasswordlessRequireResidentKey(), is(DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED));
    }

    @Test
    public void userVerificationRequirementSettings() {
        checkUserVerification();

        RealmRepresentation realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyPasswordlessUserVerificationRequirement(), is(UserVerificationRequirement.DISCOURAGED.getValue()));

        realm.setWebAuthnPolicyPasswordlessUserVerificationRequirement(null);
        testRealmResource().update(realm);

        realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyPasswordlessUserVerificationRequirement(), is(DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED));
    }

    @Test
    public void timeoutSettings() {
        checkTimeout();

        RealmRepresentation realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyPasswordlessCreateTimeout(), is(500));
    }

    @Test
    public void avoidSameAuthenticatorRegistrationSettings() {
        checkAvoidSameAuthenticatorRegistration();

        final RealmRepresentation realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());
        assertThat(realm.isWebAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister(), is(false));
    }

    @Test
    public void acceptableAaguidSettings() {
        checkAcceptableAaguid();

        RealmRepresentation realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());
        assertThat(realm.getWebAuthnPolicyPasswordlessAcceptableAaguids(), is(getAcceptableAaguid(getPolicyPage().getAcceptableAaguid())));
    }
}
