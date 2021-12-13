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
import org.keycloak.testsuite.webauthn.updaters.AbstractWebAuthnRealmUpdater;
import org.keycloak.testsuite.webauthn.updaters.WebAuthnRealmAttributeUpdater;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.webauthn4j.data.AttestationConveyancePreference.INDIRECT;
import static com.webauthn4j.data.AuthenticatorAttachment.CROSS_PLATFORM;
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
public class WebAuthnPolicySettingsTest extends AbstractWebAuthnPolicySettingsTest {

    @Page
    WebAuthnPolicyPage webAuthnPolicyPage;

    @Override
    protected WebAuthnPolicyPage getPolicyPage() {
        return webAuthnPolicyPage;
    }

    @Override
    protected AbstractWebAuthnRealmUpdater<WebAuthnRealmAttributeUpdater> getWebAuthnRealmUpdater() {
        return new WebAuthnRealmAttributeUpdater(testRealmResource());
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
            RealmRepresentation realm = testRealmResource().toRepresentation();
            assertThat(realm, notNullValue());

            assertThat(realm.getWebAuthnPolicySignatureAlgorithms(), hasItems("ES256"));
            assertThat(realm.getWebAuthnPolicyAttestationConveyancePreference(), is(INDIRECT.getValue()));
            assertThat(realm.getWebAuthnPolicyAuthenticatorAttachment(), is(CROSS_PLATFORM.getValue()));
            assertThat(realm.getWebAuthnPolicyRequireResidentKey(), is("No"));
            assertThat(realm.getWebAuthnPolicyRpId(), is(""));
            assertThat(realm.getWebAuthnPolicyUserVerificationRequirement(), is(PREFERRED.getValue()));
            assertThat(realm.getWebAuthnPolicyAcceptableAaguids(), hasItems(ALL_ZERO_AAGUID));
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

        final List<String> realmSignatureAlgs = realm.getWebAuthnPolicySignatureAlgorithms();
        assertThat(realmSignatureAlgs, notNullValue());
        assertThat(realmSignatureAlgs, hasSize(3));
        assertThat(realmSignatureAlgs, contains("ES256", "ES384", "RS1"));
    }

    @Test
    public void rpValuesSetUpInAdminConsole() {
        checkRpEntityValues();

        final RealmRepresentation realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyRpEntityName(), is(Constants.DEFAULT_WEBAUTHN_POLICY_RP_ENTITY_NAME));
        assertThat(realm.getWebAuthnPolicyRpId(), is("rpId123"));
    }

    @Test
    public void attestationConveyancePreferenceSettings() {
        checkAttestationConveyancePreference();

        // Realm
        RealmRepresentation realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyAttestationConveyancePreference(), is(AttestationConveyancePreference.NONE.getValue()));

        realm.setWebAuthnPolicyAttestationConveyancePreference(null);
        testRealmResource().update(realm);

        realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyAttestationConveyancePreference(), is(DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED));
    }

    @Test
    public void authenticatorAttachmentSettings() {
        checkAuthenticatorAttachment();

        // Realm
        RealmRepresentation realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyAuthenticatorAttachment(), is(AuthenticatorAttachment.PLATFORM.getValue()));

        realm.setWebAuthnPolicyAuthenticatorAttachment(null);
        testRealmResource().update(realm);

        realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyAuthenticatorAttachment(), is(DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED));
    }

    @Test
    public void requireResidentKeySettings() {
        checkResidentKey();

        // Realm
        RealmRepresentation realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyRequireResidentKey(), is("No"));

        realm.setWebAuthnPolicyRequireResidentKey(null);
        testRealmResource().update(realm);

        realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyRequireResidentKey(), is(DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED));
    }

    @Test
    public void userVerificationRequirementSettings() {
        checkUserVerification();

        // Realm
        RealmRepresentation realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyUserVerificationRequirement(), is(UserVerificationRequirement.DISCOURAGED.getValue()));

        realm.setWebAuthnPolicyUserVerificationRequirement(null);
        testRealmResource().update(realm);

        realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyUserVerificationRequirement(), is(DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED));
    }

    @Test
    public void timeoutSettings() {
        checkTimeout();

        // Realm
        RealmRepresentation realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());

        assertThat(realm.getWebAuthnPolicyCreateTimeout(), is(500));
    }

    @Test
    public void avoidSameAuthenticatorRegistrationSettings() {
        checkAvoidSameAuthenticatorRegistration();

        final RealmRepresentation realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());
        assertThat(realm.isWebAuthnPolicyAvoidSameAuthenticatorRegister(), is(false));
    }

    @Test
    public void acceptableAaguidSettings() {
        checkAcceptableAaguid();

        RealmRepresentation realm = testRealmResource().toRepresentation();
        assertThat(realm, notNullValue());
        assertThat(realm.getWebAuthnPolicyAcceptableAaguids(), is(getAcceptableAaguid(getPolicyPage().getAcceptableAaguid())));
    }
}
