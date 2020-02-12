/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.credential;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.converter.util.CborConverter;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.util.exception.WebAuthnException;
import com.webauthn4j.validator.WebAuthnAuthenticationContextValidationResponse;
import com.webauthn4j.validator.WebAuthnAuthenticationContextValidator;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.credential.dto.WebAuthnCredentialData;

/**
 * Credential provider for WebAuthn 2-factor credential of the user
 */
public class WebAuthnCredentialProvider implements CredentialProvider<WebAuthnCredentialModel>, CredentialInputValidator {

    private static final Logger logger = Logger.getLogger(WebAuthnCredentialProvider.class);

    private KeycloakSession session;

    private CredentialPublicKeyConverter credentialPublicKeyConverter;
    private AttestationStatementConverter attestationStatementConverter;

    public WebAuthnCredentialProvider(KeycloakSession session, CborConverter converter) {
        this.session = session;
        if (credentialPublicKeyConverter == null)
            credentialPublicKeyConverter = new CredentialPublicKeyConverter(converter);
        if (attestationStatementConverter == null)
            attestationStatementConverter = new AttestationStatementConverter(converter);
    }

    private UserCredentialStore getCredentialStore() {
        return session.userCredentialManager();
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, WebAuthnCredentialModel credentialModel) {
        if (credentialModel.getCreatedDate() == null) {
            credentialModel.setCreatedDate(Time.currentTimeMillis());
        }

        return getCredentialStore().createCredential(realm, user, credentialModel);
    }

    @Override
    public void deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        logger.debugv("Delete WebAuthn credential. username = {0}, credentialId = {1}", user.getUsername(), credentialId);
        getCredentialStore().removeStoredCredential(realm, user, credentialId);
    }

    @Override
    public WebAuthnCredentialModel getCredentialFromModel(CredentialModel model) {
        return WebAuthnCredentialModel.createFromCredentialModel(model);
    }


    /**
     * Convert WebAuthn credential input to the model, which can be saved in the persistent storage (DB)
     *
     * @param input should be typically WebAuthnCredentialModelInput
     * @param userLabel label for the credential
     */
    public WebAuthnCredentialModel getCredentialModelFromCredentialInput(CredentialInput input, String userLabel) {
        if (!supportsCredentialType(input.getType())) return null;

        WebAuthnCredentialModelInput webAuthnModel = (WebAuthnCredentialModelInput) input;

        String aaguid = webAuthnModel.getAttestedCredentialData().getAaguid().toString();
        String credentialId = Base64.encodeBytes(webAuthnModel.getAttestedCredentialData().getCredentialId());
        String credentialPublicKey = credentialPublicKeyConverter.convertToDatabaseColumn(webAuthnModel.getAttestedCredentialData().getCOSEKey());
        long counter = webAuthnModel.getCount();

        WebAuthnCredentialModel model = WebAuthnCredentialModel.create(getType(), userLabel, aaguid, credentialId, null, credentialPublicKey, counter);

        model.setId(webAuthnModel.getCredentialDBId());

        return model;
    }


    /**
     * Convert WebAuthnCredentialModel, which was usually retrieved from DB, to the CredentialInput, which contains data in the webauthn4j specific format
     */
    private WebAuthnCredentialModelInput getCredentialInputFromCredentialModel(CredentialModel credential) {
        WebAuthnCredentialModel webAuthnCredential = getCredentialFromModel(credential);

        WebAuthnCredentialData credData = webAuthnCredential.getWebAuthnCredentialData();

        WebAuthnCredentialModelInput auth = new WebAuthnCredentialModelInput(getType());

        byte[] credentialId = null;
        try {
            credentialId = Base64.decode(credData.getCredentialId());
        } catch (IOException ioe) {
            // NOP
        }

        AAGUID aaguid = new AAGUID(credData.getAaguid());

        COSEKey pubKey = credentialPublicKeyConverter.convertToEntityAttribute(credData.getCredentialPublicKey());

        AttestedCredentialData attrCredData = new AttestedCredentialData(aaguid, credentialId, pubKey);

        auth.setAttestedCredentialData(attrCredData);

        long count = credData.getCounter();
        auth.setCount(count);

        auth.setCredentialDBId(credential.getId());

        return auth;
    }


    @Override
    public boolean supportsCredentialType(String credentialType) {
        return getType().equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) return false;
        return !session.userCredentialManager().getStoredCredentialsByType(realm, user, credentialType).isEmpty();
    }


    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!WebAuthnCredentialModelInput.class.isInstance(input)) return false;

        WebAuthnCredentialModelInput context = WebAuthnCredentialModelInput.class.cast(input);
        List<WebAuthnCredentialModelInput> auths = getWebAuthnCredentialModelList(realm, user);

        WebAuthnAuthenticationContextValidator webAuthnAuthenticationContextValidator =
                new WebAuthnAuthenticationContextValidator();
        try {
            for (WebAuthnCredentialModelInput auth : auths) {

                byte[] credentialId = auth.getAttestedCredentialData().getCredentialId();
                if (Arrays.equals(credentialId, context.getAuthenticationContext().getCredentialId())) {
                    Authenticator authenticator = new AuthenticatorImpl(
                            auth.getAttestedCredentialData(),
                            auth.getAttestationStatement(),
                            auth.getCount()
                    );

                    // WebAuthnException is thrown if validation fails
                    WebAuthnAuthenticationContextValidationResponse response =
                            webAuthnAuthenticationContextValidator.validate(
                                    context.getAuthenticationContext(),
                                    authenticator);

                    logger.debugv("response.getAuthenticatorData().getFlags() = {0}", response.getAuthenticatorData().getFlags());

                    // update authenticator counter
                    long count = auth.getCount();
                    CredentialModel credModel = getCredentialStore().getStoredCredentialById(realm, user, auth.getCredentialDBId());
                    WebAuthnCredentialModel webAuthnCredModel = getCredentialFromModel(credModel);
                    webAuthnCredModel.updateCounter(count + 1);
                    getCredentialStore().updateCredential(realm, user, webAuthnCredModel);

                    logger.debugf("Successfully validated WebAuthn credential for user %s", user.getUsername());
                    dumpCredentialModel(webAuthnCredModel, auth);

                    return true;
                }
            }
        } catch (WebAuthnException wae) {
            wae.printStackTrace();
            throw(wae);
        }
        // no authenticator matched
        return false;
    }


    @Override
    public String getType() {
        return WebAuthnCredentialModel.TYPE_TWOFACTOR;
    }


    private List<WebAuthnCredentialModelInput> getWebAuthnCredentialModelList(RealmModel realm, UserModel user) {
        List<CredentialModel> credentialModels = session.userCredentialManager().getStoredCredentialsByType(realm, user, getType());

        return credentialModels.stream()
                .map(this::getCredentialInputFromCredentialModel)
                .collect(Collectors.toList());
    }

    public void dumpCredentialModel(WebAuthnCredentialModel credential, WebAuthnCredentialModelInput auth) {
        if(logger.isDebugEnabled()) {
            logger.debug("  Persisted Credential Info::");
            logger.debug(credential);
            logger.debug("  Context Credential Info::");
            logger.debug(auth);
        }
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext) {
        return CredentialTypeMetadata.builder()
                .type(getType())
                .category(CredentialTypeMetadata.Category.TWO_FACTOR)
                .displayName("webauthn-display-name")
                .helpText("webauthn-help-text")
                .iconCssClass("kcAuthenticatorWebAuthnClass")
                .createAction(WebAuthnRegisterFactory.PROVIDER_ID)
                .removeable(true)
                .build(session);
    }

    protected KeycloakSession getKeycloakSession() {
        return session;
    }

}
