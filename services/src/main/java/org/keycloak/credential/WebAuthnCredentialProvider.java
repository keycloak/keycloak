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
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;

import org.keycloak.authentication.authenticators.browser.WebAuthnMetadataService;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.WebAuthnPolicy;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.credential.dto.WebAuthnCredentialData;
import org.keycloak.models.credential.dto.WebAuthnCredentialPresentationData;
import org.keycloak.util.JsonSerialization;

import com.webauthn4j.WebAuthnAuthenticationManager;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticatorTransport;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.client.CollectedClientData;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.util.AssertUtil;
import com.webauthn4j.util.exception.WebAuthnException;
import com.webauthn4j.verifier.OriginVerifierImpl;
import com.webauthn4j.verifier.exception.BadOriginException;
import org.jboss.logging.Logger;

/**
 * Credential provider for WebAuthn 2-factor credential of the user
 */
public class WebAuthnCredentialProvider implements CredentialProvider<WebAuthnCredentialModel>, CredentialInputValidator {

    private static final Logger logger = Logger.getLogger(WebAuthnCredentialProvider.class);
    private static final String WEBAUTHN_AUTHENTICATOR_PROVIDER = "webauthn-authenticator-provider";
    private static final String WEBAUTHN_TRANSPORTS = "webauthn-transports";

    private final KeycloakSession session;
    private final WebAuthnMetadataService metadataService;
    private final CredentialPublicKeyConverter credentialPublicKeyConverter;
    private final AttestationStatementConverter attestationStatementConverter;

    public WebAuthnCredentialProvider(KeycloakSession session, WebAuthnMetadataService metadataService, ObjectConverter objectConverter) {
        this.session = session;
        this.metadataService = metadataService;
        this.credentialPublicKeyConverter = new CredentialPublicKeyConverter(objectConverter);
        this.attestationStatementConverter = new AttestationStatementConverter(objectConverter);
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, WebAuthnCredentialModel credentialModel) {
        if (credentialModel.getCreatedDate() == null) {
            credentialModel.setCreatedDate(Time.currentTimeMillis());
        }

        return user.credentialManager().createStoredCredential(credentialModel);
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        logger.debugv("Delete WebAuthn credential. username = {0}, credentialId = {1}", user.getUsername(), credentialId);
        return user.credentialManager().removeStoredCredentialById(credentialId);
    }

    @Override
    public WebAuthnCredentialModel getCredentialFromModel(CredentialModel model) {
        return WebAuthnCredentialModel.createFromCredentialModel(model);
    }

    @Override
    public WebAuthnCredentialModel getCredentialForPresentationFromModel(CredentialModel model) {
        WebAuthnCredentialModel origCredential = getCredentialFromModel(model);
        WebAuthnCredentialData data = origCredential.getWebAuthnCredentialData();

        String authenticatorProvider = metadataService.getAuthenticatorProvider(data.getAaguid());
        if (authenticatorProvider == null)
            return origCredential;

        WebAuthnCredentialPresentationData presentationData = new WebAuthnCredentialPresentationData(
                data.getAaguid(), data.getCredentialId(), data.getCounter(), data.getAttestationStatement(), data.getCredentialPublicKey(),
                data.getAttestationStatementFormat(), data.getTransports(), authenticatorProvider);
        return WebAuthnCredentialModel.create(origCredential.getId(), origCredential.getType(), origCredential.getCreatedDate(), origCredential.getUserLabel(),
                presentationData, origCredential.getWebAuthnSecretData());
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
        String credentialId = Base64.getEncoder().encodeToString(webAuthnModel.getAttestedCredentialData().getCredentialId());
        String credentialPublicKey = credentialPublicKeyConverter.convertToDatabaseColumn(webAuthnModel.getAttestedCredentialData().getCOSEKey());
        long counter = webAuthnModel.getCount();
        String attestationStatementFormat = webAuthnModel.getAttestationStatementFormat();

        final Set<String> transports = webAuthnModel.getTransports()
                .stream()
                .map(AuthenticatorTransport::getValue)
                .collect(Collectors.toSet());

        WebAuthnCredentialModel model = WebAuthnCredentialModel.create(
                getType(),
                userLabel,
                aaguid,
                credentialId,
                null,
                credentialPublicKey,
                counter,
                attestationStatementFormat,
                transports
        );

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
            credentialId = Base64.getDecoder().decode(credData.getCredentialId());
        } catch (IllegalArgumentException ex) {
            // NOP
        }

        AAGUID aaguid = new AAGUID(credData.getAaguid());

        COSEKey pubKey = credentialPublicKeyConverter.convertToEntityAttribute(credData.getCredentialPublicKey());

        AttestedCredentialData attrCredData = new AttestedCredentialData(aaguid, credentialId, pubKey);

        auth.setAttestedCredentialData(attrCredData);

        long count = credData.getCounter();
        auth.setCount(count);

        auth.setCredentialDBId(credential.getId());

        auth.setAttestationStatementFormat(credData.getAttestationStatementFormat());

        return auth;
    }


    @Override
    public boolean supportsCredentialType(String credentialType) {
        return getType().equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) return false;
        return user.credentialManager().getStoredCredentialsByTypeStream(credentialType).count() > 0;
    }


    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!WebAuthnCredentialModelInput.class.isInstance(input)) return false;

        WebAuthnCredentialModelInput context = WebAuthnCredentialModelInput.class.cast(input);
        List<WebAuthnCredentialModelInput> auths = getWebAuthnCredentialModelList(realm, user);

        WebAuthnAuthenticationManager webAuthnAuthenticationManager = getWebAuthnAuthenticationManager();
        AuthenticationData authenticationData = null;

        try {
            for (WebAuthnCredentialModelInput auth : auths) {

                byte[] credentialId = auth.getAttestedCredentialData().getCredentialId();
                if (Arrays.equals(credentialId, context.getAuthenticationRequest().getCredentialId())) {
                    Authenticator authenticator = new AuthenticatorImpl(
                            auth.getAttestedCredentialData(),
                            auth.getAttestationStatement(),
                            auth.getCount()
                    );

                    // parse
                    authenticationData = webAuthnAuthenticationManager.parse(context.getAuthenticationRequest());
                    // validate
                    AuthenticationParameters authenticationParameters = new AuthenticationParameters(
                            context.getAuthenticationParameters().getServerProperty(),
                            authenticator,
                            context.getAuthenticationParameters().isUserVerificationRequired()
                    );
                    webAuthnAuthenticationManager.verify(authenticationData, authenticationParameters);


                    logger.debugv("response.getAuthenticatorData().getFlags() = {0}", authenticationData.getAuthenticatorData().getFlags());

                    CredentialModel credModel = user.credentialManager().getStoredCredentialById(auth.getCredentialDBId());
                    WebAuthnCredentialModel webAuthnCredModel = getCredentialFromModel(credModel);

                    // update authenticator counter
                    // counters are an optional feature of the spec - if an authenticator does not support them, it
                    // will always send zero. MacOS/iOS does this for keys stored in the secure enclave (TouchID/FaceID)
                    long count = auth.getCount();
                    if (count > 0) {
                        webAuthnCredModel.updateCounter(count + 1);
                        user.credentialManager().updateStoredCredential(webAuthnCredModel);
                    }

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

    protected WebAuthnAuthenticationManager getWebAuthnAuthenticationManager() {
        WebAuthnPolicy policy = getWebAuthnPolicy();
        Set<Origin> origins = policy.getExtraOrigins().stream()
                .map(Origin::new)
                .collect(Collectors.toSet());
        WebAuthnAuthenticationManager webAuthnAuthenticationManager = new WebAuthnAuthenticationManager();
        webAuthnAuthenticationManager.getAuthenticationDataVerifier().setOriginVerifier(new OriginVerifierImpl() {
            @Override
            protected void verify(@Nonnull CollectedClientData collectedClientData,
                                    @Nonnull ServerProperty serverProperty) {
                AssertUtil.notNull(collectedClientData, "collectedClientData must not be null");
                AssertUtil.notNull(serverProperty, "serverProperty must not be null");
                final Origin clientOrigin = collectedClientData.getOrigin();
                if (serverProperty.getOrigins().contains(clientOrigin)) return;
                // https://github.com/w3c/webauthn/issues/1297
                if (origins.contains(clientOrigin)) return;
                throw new BadOriginException("The collectedClientData '" + clientOrigin + "' origin doesn't match any of the preconfigured origins.");
            }
        });
        return webAuthnAuthenticationManager;
    }

    protected WebAuthnPolicy getWebAuthnPolicy() {
        return session.getContext().getRealm().getWebAuthnPolicy();
    }

    @Override
    public String getType() {
        return WebAuthnCredentialModel.TYPE_TWOFACTOR;
    }


    private List<WebAuthnCredentialModelInput> getWebAuthnCredentialModelList(RealmModel realm, UserModel user) {
        return user.credentialManager().getStoredCredentialsByTypeStream(getType())
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
    @Override
    public CredentialMetadata getCredentialMetadata(WebAuthnCredentialModel credentialModel, CredentialTypeMetadata credentialTypeMetadata) {

        CredentialMetadata credentialMetadata = new CredentialMetadata();
        List<CredentialMetadata.LocalizedMessage> properties = new LinkedList<>();

        try {
            credentialModel = getCredentialForPresentationFromModel(credentialModel);
            WebAuthnCredentialPresentationData credentialData = JsonSerialization.readValue(credentialModel.getCredentialData(), WebAuthnCredentialPresentationData.class);
            Set<String> transports = credentialData.getTransports();
            if (credentialData.getAuthenticatorProvider() != null) {
                properties.add(new CredentialMetadata.LocalizedMessage(WEBAUTHN_AUTHENTICATOR_PROVIDER, new String[] { credentialData.getAuthenticatorProvider() }));
            }
            if (transports != null && !transports.isEmpty()) {
                String joinedTransports = String.join(", ", transports);
                properties.add(new CredentialMetadata.LocalizedMessage(WEBAUTHN_TRANSPORTS, new String[] { joinedTransports }));
            }
            if (!properties.isEmpty()) {
                credentialMetadata.setInfoProperties(properties);
            }
        } catch (
                IOException e) {
            logger.warn("unable to deserialize model information, skipping messages", e);
        }

        credentialMetadata.setCredentialModel(credentialModel);
        return credentialMetadata;
    }

}
