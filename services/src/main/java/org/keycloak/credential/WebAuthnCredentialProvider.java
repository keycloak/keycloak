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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.WebAuthnConstants;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.converter.util.CborConverter;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.CredentialPublicKey;
import com.webauthn4j.data.attestation.statement.AttestationStatement;
import com.webauthn4j.util.exception.WebAuthnException;
import com.webauthn4j.validator.WebAuthnAuthenticationContextValidationResponse;
import com.webauthn4j.validator.WebAuthnAuthenticationContextValidator;

public class WebAuthnCredentialProvider implements CredentialProvider, CredentialInputValidator, CredentialInputUpdater {

    private static final Logger logger = Logger.getLogger(WebAuthnCredentialProvider.class);

    private static final String ATTESTATION_STATEMENT = "ATTESTATION_STATEMENT";
    private static final String AAGUID = "AAGUID";
    private static final String CREDENTIAL_ID = "CREDENTIAL_ID";
    private static final String CREDENTIAL_PUBLIC_KEY = "CREDENTIAL_PUBLIC_KEY";

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

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (input == null) return false;
        CredentialModel model = createCredentialModel(input);
        if (model == null) return false;
        session.userCredentialManager().createCredential(realm, user, model);
        return true;
    }

    private CredentialModel createCredentialModel(CredentialInput input) {
        if (!supportsCredentialType(input.getType())) return null;

        WebAuthnCredentialModel webAuthnModel = (WebAuthnCredentialModel) input;
        CredentialModel model = new CredentialModel();
        model.setType(WebAuthnCredentialModel.WEBAUTHN_CREDENTIAL_TYPE);
        model.setCreatedDate(Time.currentTimeMillis());

        MultivaluedHashMap<String, String> credential = new MultivaluedHashMap<>();

        credential.add(ATTESTATION_STATEMENT, attestationStatementConverter.convertToDatabaseColumn(webAuthnModel.getAttestationStatement()));

        credential.add(AAGUID, webAuthnModel.getAttestedCredentialData().getAaguid().toString());

        credential.add(CREDENTIAL_ID, Base64.encodeBytes(webAuthnModel.getAttestedCredentialData().getCredentialId()));

        credential.add(CREDENTIAL_PUBLIC_KEY, credentialPublicKeyConverter.convertToDatabaseColumn(webAuthnModel.getAttestedCredentialData().getCredentialPublicKey()));

        model.setId(webAuthnModel.getAuthenticatorId());

        model.setConfig(credential);

        // authenticator's counter
        model.setValue(String.valueOf(webAuthnModel.getCount()));

        if(logger.isDebugEnabled()) {
            dumpCredentialModel(model);
            dumpWebAuthnCredentialModel(webAuthnModel);
        }

        return model;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) return;
        // delete webauthn authenticator's credential itself
        for (CredentialModel credential : session.userCredentialManager().getStoredCredentialsByType(realm, user, credentialType)) {
            logger.infov("Delete public key credential. username = {0}, credentialType = {1}", user.getUsername(), credentialType);
            dumpCredentialModel(credential);
            session.userCredentialManager().removeStoredCredential(realm, user, credential.getId());
        }
        // delete webauthn authenticator's metadata
        user.removeAttribute(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR);
        user.removeAttribute(WebAuthnConstants.PUBKEY_CRED_ID_ATTR);
        user.removeAttribute(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR);
    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        return isConfiguredFor(realm, user, WebAuthnCredentialModel.WEBAUTHN_CREDENTIAL_TYPE) ? Collections.singleton(WebAuthnCredentialModel.WEBAUTHN_CREDENTIAL_TYPE) : Collections.emptySet();
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return WebAuthnCredentialModel.WEBAUTHN_CREDENTIAL_TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) return false;
        return !session.userCredentialManager().getStoredCredentialsByType(realm, user, credentialType).isEmpty();
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!WebAuthnCredentialModel.class.isInstance(input)) return false;

        WebAuthnCredentialModel context = WebAuthnCredentialModel.class.cast(input);
        List<WebAuthnCredentialModel> auths = getWebAuthnCredentialModelList(realm, user);

        WebAuthnAuthenticationContextValidator webAuthnAuthenticationContextValidator =
                new WebAuthnAuthenticationContextValidator();
        try {
            for (WebAuthnCredentialModel auth : auths) {

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

                    logger.infov("response.getAuthenticatorData().getFlags() = {0}", response.getAuthenticatorData().getFlags());

                    // update authenticator counter
                    long count = auth.getCount();
                    auth.setCount(count + 1);
                    CredentialModel cred = createCredentialModel(auth);
                    session.userCredentialManager().updateCredential(realm, user, cred);

                    if(logger.isDebugEnabled()) {
                        dumpCredentialModel(cred);
                        dumpWebAuthnCredentialModel(auth);
                    }

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

    private List<WebAuthnCredentialModel> getWebAuthnCredentialModelList(RealmModel realm, UserModel user) {
        List<WebAuthnCredentialModel> auths = new ArrayList<>();
        for (CredentialModel credential : session.userCredentialManager().getStoredCredentialsByType(realm, user, WebAuthnCredentialModel.WEBAUTHN_CREDENTIAL_TYPE)) {
            WebAuthnCredentialModel auth = new WebAuthnCredentialModel();
            MultivaluedHashMap<String, String> attributes = credential.getConfig();

            AttestationStatement attrStatement = attestationStatementConverter.convertToEntityAttribute(attributes.getFirst(ATTESTATION_STATEMENT));
            auth.setAttestationStatement(attrStatement);

            AAGUID aaguid = new AAGUID(attributes.getFirst(AAGUID));

            byte[] credentialId = null;
            try {
                credentialId = Base64.decode(attributes.getFirst(CREDENTIAL_ID));
            } catch (IOException ioe) {
                // NOP
            }

            CredentialPublicKey pubKey = credentialPublicKeyConverter.convertToEntityAttribute(attributes.getFirst(CREDENTIAL_PUBLIC_KEY));

            AttestedCredentialData attrCredData = new AttestedCredentialData(aaguid, credentialId, pubKey);

            auth.setAttestedCredentialData(attrCredData);

            long count = Long.parseLong(credential.getValue());
            auth.setCount(count);

            auth.setAuthenticatorId(credential.getId());

            auths.add(auth);
        }
        return auths;
    }

    private void dumpCredentialModel(CredentialModel credential) {
        logger.debugv("  Persisted Credential Info::");
        MultivaluedHashMap<String, String> attributes = credential.getConfig();
        logger.debugv("    ATTESTATION_STATEMENT = {0}", attributes.getFirst(ATTESTATION_STATEMENT));
        logger.debugv("    AAGUID = {0}", attributes.getFirst(AAGUID));
        logger.debugv("    CREDENTIAL_ID = {0}", attributes.getFirst(CREDENTIAL_ID));
        logger.debugv("    CREDENTIAL_PUBLIC_KEY = {0}", attributes.getFirst(CREDENTIAL_PUBLIC_KEY));
        logger.debugv("    count = {0}", credential.getValue());
        logger.debugv("    authenticator_id = {0}", credential.getId());
    }

    private void dumpWebAuthnCredentialModel(WebAuthnCredentialModel auth) {
        logger.debug("  Context Credential Info::");
        logger.debug(auth);
    }

}
