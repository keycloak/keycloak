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

package org.keycloak.authentication.requiredactions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.CredentialRegistrator;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.common.util.UriUtils;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.WebAuthnCredentialModelInput;
import org.keycloak.credential.WebAuthnCredentialProvider;
import org.keycloak.credential.WebAuthnCredentialProviderFactory;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.WebAuthnPolicy;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

import com.webauthn4j.WebAuthnRegistrationManager;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.AuthenticatorTransport;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.statement.AttestationStatement;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.util.exception.WebAuthnException;
import com.webauthn4j.verifier.attestation.statement.AttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.androidkey.AndroidKeyAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.androidsafetynet.AndroidSafetyNetAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.none.NoneAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.packed.PackedAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.tpm.TPMAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.u2f.FIDOU2FAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.trustworthiness.certpath.CertPathTrustworthinessVerifier;
import com.webauthn4j.verifier.attestation.trustworthiness.self.DefaultSelfAttestationTrustworthinessVerifier;
import org.jboss.logging.Logger;

import static org.keycloak.WebAuthnConstants.REG_ERR_DETAIL_LABEL;
import static org.keycloak.WebAuthnConstants.REG_ERR_LABEL;
import static org.keycloak.services.messages.Messages.WEBAUTHN_ERROR_REGISTER_VERIFICATION;
import static org.keycloak.services.messages.Messages.WEBAUTHN_ERROR_REGISTRATION;
import static org.keycloak.services.messages.Messages.WEBAUTHN_REGISTER_TITLE;

/**
 * Required action for register WebAuthn 2-factor credential for the user
 */
public class WebAuthnRegister implements RequiredActionProvider, CredentialRegistrator {

    private static final String WEB_AUTHN_TITLE_ATTR = "webAuthnTitle";
    private static final Logger logger = Logger.getLogger(WebAuthnRegister.class);

    private final KeycloakSession session;
    private final CertPathTrustworthinessVerifier certPathtrustVerifier;

    public WebAuthnRegister(KeycloakSession session, CertPathTrustworthinessVerifier certPathtrustVerifier) {
        this.session = session;
        this.certPathtrustVerifier = certPathtrustVerifier;
    }

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        String actionParameter = context.getAuthenticationSession().getClientNote(Constants.KC_ACTION_PARAMETER);
        UserModel userModel = context.getUser();
        if (Constants.KC_ACTION_PARAMETER_SKIP_IF_EXISTS.equals(actionParameter)
                && userModel.credentialManager().getStoredCredentialsByTypeStream(getCredentialType()).findAny().isPresent()) {
            context.success();
            return;
        }

        // Use standard UTF-8 charset to get bytes from string.
        // Otherwise the platform's default charset is used and it might cause problems later when
        // decoded on different system.
        String userId = Base64Url.encode(userModel.getId().getBytes(StandardCharsets.UTF_8));
        String username = userModel.getUsername();
        Challenge challenge = new DefaultChallenge();
        String challengeValue = Base64Url.encode(challenge.getValue());
        context.getAuthenticationSession().setAuthNote(WebAuthnConstants.AUTH_CHALLENGE_NOTE, challengeValue);

        // construct parameters for calling WebAuthn API navigator.credential.create()

        // mandatory
        WebAuthnPolicy policy = getWebAuthnPolicy(context);
        List<String> signatureAlgorithmsList = policy.getSignatureAlgorithm();
        // Convert human-readable algorithms to their COSE identifier form
        List<Long> signatureAlgorithms = convertSignatureAlgorithms(signatureAlgorithmsList);
        String rpEntityName = policy.getRpEntityName();

        // optional
        String rpId = policy.getRpId();
        if (rpId == null || rpId.isEmpty()) rpId =  context.getUriInfo().getBaseUri().getHost();
        String attestationConveyancePreference = policy.getAttestationConveyancePreference();
        String authenticatorAttachment = policy.getAuthenticatorAttachment();
        String requireResidentKey = policy.getRequireResidentKey();
        String userVerificationRequirement = policy.getUserVerificationRequirement();
        long createTimeout = policy.getCreateTimeout();
        boolean avoidSameAuthenticatorRegister = policy.isAvoidSameAuthenticatorRegister();

        String excludeCredentialIds = "";
        if (avoidSameAuthenticatorRegister) {
            excludeCredentialIds = userModel.credentialManager().getStoredCredentialsByTypeStream(getCredentialType())
                    .map(credentialModel -> {
                        WebAuthnCredentialModel credModel = WebAuthnCredentialModel.createFromCredentialModel(credentialModel);
                        return Base64Url.encodeBase64ToBase64Url(credModel.getWebAuthnCredentialData().getCredentialId());
                    }).collect(Collectors.joining(","));
        }

        String isSetRetry = null;

        if (isFormDataRequest(context.getHttpRequest())) {
            isSetRetry = context.getHttpRequest().getDecodedFormParameters().getFirst(WebAuthnConstants.IS_SET_RETRY);
        }

        Response form = context.form()
                .setAttribute(WebAuthnConstants.CHALLENGE, challengeValue)
                .setAttribute(WebAuthnConstants.USER_ID, userId)
                .setAttribute(WebAuthnConstants.USER_NAME, username)
                .setAttribute(WebAuthnConstants.RP_ENTITY_NAME, rpEntityName)
                .setAttribute(WebAuthnConstants.SIGNATURE_ALGORITHMS, signatureAlgorithms)
                .setAttribute(WebAuthnConstants.RP_ID, rpId)
                .setAttribute(WebAuthnConstants.ATTESTATION_CONVEYANCE_PREFERENCE, attestationConveyancePreference)
                .setAttribute(WebAuthnConstants.AUTHENTICATOR_ATTACHMENT, authenticatorAttachment)
                .setAttribute(WebAuthnConstants.REQUIRE_RESIDENT_KEY, requireResidentKey)
                .setAttribute(WebAuthnConstants.USER_VERIFICATION_REQUIREMENT, userVerificationRequirement)
                .setAttribute(WebAuthnConstants.CREATE_TIMEOUT, createTimeout)
                .setAttribute(WebAuthnConstants.EXCLUDE_CREDENTIAL_IDS, excludeCredentialIds)
                .setAttribute(WebAuthnConstants.IS_SET_RETRY, isSetRetry)
                .createForm("webauthn-register.ftl");
        context.challenge(form);
    }

    protected WebAuthnPolicy getWebAuthnPolicy(RequiredActionContext context) {
        return context.getRealm().getWebAuthnPolicy();
    }

    @Override
    public String getCredentialType(KeycloakSession session, AuthenticationSessionModel authenticationSession) {
        return getCredentialType();
    }

    protected String getCredentialType() {
        return WebAuthnCredentialModel.TYPE_TWOFACTOR;
    }

    protected String getCredentialProviderId() {
        return WebAuthnCredentialProviderFactory.PROVIDER_ID;
    }

    /**
     * Method to provide removal and deprecation hint
     * @deprecated For compatibility sake as long as we use @link {@link EventType#UPDATE_PASSWORD} , {@link EventType#UPDATE_TOTP} a.s.o.
     */
    @Deprecated
    protected EventType getOriginalEventTypeForBackwardsCompatibility(RequiredActionContext context) {
        return context.getEvent().getEvent().getType();
    }

    @Override
    public void processAction(RequiredActionContext context) {

        MultivaluedMap<String, String> params = context.getHttpRequest().getDecodedFormParameters();

        String isSetRetry = params.getFirst(WebAuthnConstants.IS_SET_RETRY);
        if (isSetRetry != null && !isSetRetry.isEmpty()) {
            requiredActionChallenge(context);
            return;
        }

        EventType originalEventType = getOriginalEventTypeForBackwardsCompatibility(context);
        context.getEvent()
                .event(EventType.UPDATE_CREDENTIAL)
                .detail(Details.CREDENTIAL_TYPE, getCredentialType());

        // receive error from navigator.credentials.create()
        String errorMsgFromWebAuthnApi = params.getFirst(WebAuthnConstants.ERROR);
        if (errorMsgFromWebAuthnApi != null && !errorMsgFromWebAuthnApi.isEmpty()) {
            setErrorResponse(context, WEBAUTHN_ERROR_REGISTER_VERIFICATION, errorMsgFromWebAuthnApi, originalEventType);
            return;
        }

        WebAuthnPolicy policy = getWebAuthnPolicy(context);
        String rpId = policy.getRpId();
        if (rpId == null || rpId.isEmpty()) rpId =  context.getUriInfo().getBaseUri().getHost();
        String label = params.getFirst(WebAuthnConstants.AUTHENTICATOR_LABEL);
        byte[] clientDataJSON = Base64.getUrlDecoder().decode(params.getFirst(WebAuthnConstants.CLIENT_DATA_JSON));
        byte[] attestationObject = Base64.getUrlDecoder().decode(params.getFirst(WebAuthnConstants.ATTESTATION_OBJECT));

        String publicKeyCredentialId = params.getFirst(WebAuthnConstants.PUBLIC_KEY_CREDENTIAL_ID);

        Origin origin = new Origin(UriUtils.getOrigin(context.getUriInfo().getBaseUri()));
        Set<Origin> allOrigins = policy
                .getExtraOrigins()
                .stream()
                .map(Origin::new)
                .collect(Collectors.toSet());
        allOrigins.add(origin);
        Challenge challenge = new DefaultChallenge(context.getAuthenticationSession().getAuthNote(WebAuthnConstants.AUTH_CHALLENGE_NOTE));
        ServerProperty serverProperty = new ServerProperty(allOrigins, rpId, challenge, null);
        // check User Verification by considering a malicious user might modify the result of calling WebAuthn API
        boolean isUserVerificationRequired = policy.getUserVerificationRequirement().equals(Constants.WEBAUTHN_POLICY_OPTION_REQUIRED);

        final String transportsParam = params.getFirst(WebAuthnConstants.TRANSPORTS);

        RegistrationRequest registrationRequest;

        if (StringUtil.isNotBlank(transportsParam)) {
            final Set<String> transports = new HashSet<>(Arrays.asList(transportsParam.split(",")));
            registrationRequest = new RegistrationRequest(attestationObject, clientDataJSON, transports);
        } else {
            registrationRequest = new RegistrationRequest(attestationObject, clientDataJSON);
        }

        RegistrationParameters registrationParameters = new RegistrationParameters(serverProperty, isUserVerificationRequired);

        if ("on".equals(params.getFirst("logout-sessions"))) {
            AuthenticatorUtil.logoutOtherSessions(context);
        }

        WebAuthnRegistrationManager webAuthnRegistrationManager = createWebAuthnRegistrationManager(policy.getAttestationConveyancePreference());
        try {
            // parse
            RegistrationData registrationData = webAuthnRegistrationManager.parse(registrationRequest);
            // validate
            webAuthnRegistrationManager.verify(registrationData, registrationParameters);

            showInfoAfterWebAuthnApiCreate(registrationData);

            checkAcceptedAuthenticator(registrationData, policy);

            WebAuthnCredentialModelInput credential = new WebAuthnCredentialModelInput(getCredentialType());

            credential.setAttestedCredentialData(registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData());
            credential.setCount(registrationData.getAttestationObject().getAuthenticatorData().getSignCount());
            credential.setAttestationStatementFormat(registrationData.getAttestationObject().getFormat());
            credential.setTransports(registrationData.getTransports());

            // Save new webAuthn credential
            WebAuthnCredentialProvider webAuthnCredProvider = (WebAuthnCredentialProvider) this.session.getProvider(CredentialProvider.class, getCredentialProviderId());
            WebAuthnCredentialModel newCredentialModel = webAuthnCredProvider.getCredentialModelFromCredentialInput(credential, label);

            webAuthnCredProvider.createCredential(context.getRealm(), context.getUser(), newCredentialModel);

            String aaguid = newCredentialModel.getWebAuthnCredentialData().getAaguid();
            logger.debugv("WebAuthn credential registration success for user {0}. credentialType = {1}, publicKeyCredentialId = {2}, publicKeyCredentialLabel = {3}, publicKeyCredentialAAGUID = {4}",
                    context.getUser().getUsername(), getCredentialType(), publicKeyCredentialId, label, aaguid);
            webAuthnCredProvider.dumpCredentialModel(newCredentialModel, credential);

            context.getEvent()
                .detail(WebAuthnConstants.PUBKEY_CRED_ID_ATTR, publicKeyCredentialId)
                .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, label)
                .detail(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR, aaguid);
            context.getEvent().clone().event(originalEventType).success();
            context.success();
        } catch (WebAuthnException wae) {
            if (logger.isDebugEnabled()) logger.debug(wae.getMessage(), wae);
            setErrorResponse(context, WEBAUTHN_ERROR_REGISTRATION, wae.getMessage(), originalEventType);
            return;
        } catch (Exception e) {
            if (logger.isDebugEnabled()) logger.debug(e.getMessage(), e);
            setErrorResponse(context, WEBAUTHN_ERROR_REGISTRATION, e.getMessage(), originalEventType);
            return;
        }
    }

    /**
     * Create WebAuthnRegistrationManager instance
     * Can be overridden in subclasses to customize the used attestation validators
     *
     * @param attestationPreference The attestation selected in the policy
     * @return webauthn4j WebAuthnRegistrationManager instance
     */
    protected WebAuthnRegistrationManager createWebAuthnRegistrationManager(String attestationPreference) {
        List<AttestationStatementVerifier> verifiers = new ArrayList<>(6);
        if (attestationPreference == null
                || Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED.equals(attestationPreference)
                || AttestationConveyancePreference.NONE.getValue().equals(attestationPreference)) {
            verifiers.add(new NoneAttestationStatementVerifier());
        }
        verifiers.add(new PackedAttestationStatementVerifier());
        verifiers.add(new TPMAttestationStatementVerifier());
        verifiers.add(new AndroidKeyAttestationStatementVerifier());
        verifiers.add(new AndroidSafetyNetAttestationStatementVerifier());
        verifiers.add(new FIDOU2FAttestationStatementVerifier());

        return new WebAuthnRegistrationManager(
                verifiers,
                this.certPathtrustVerifier,
                new DefaultSelfAttestationTrustworthinessVerifier(),
                Collections.emptyList(), // Custom Registration Verifier is not supported
                new ObjectConverter()
        );
    }

    /**
     * Converts a list of human-readable webauthn signature methods (ES256, RS256, etc) into
     * their <a href="https://www.iana.org/assignments/cose/cose.xhtml#algorithms"> COSE identifier</a> form.
     *
     * Returns the list of converted algorithm identifiers.
    **/
    private List<Long> convertSignatureAlgorithms(List<String> signatureAlgorithmsList) {
        List<Long> algs = new ArrayList();
        if (signatureAlgorithmsList == null || signatureAlgorithmsList.isEmpty()) return algs;

        for (String s : signatureAlgorithmsList) {
            switch (s) {
            case Algorithm.ES256 :
                algs.add(COSEAlgorithmIdentifier.ES256.getValue());
                break;
            case Algorithm.RS256 :
                algs.add(COSEAlgorithmIdentifier.RS256.getValue());
                break;
            case Algorithm.ES384 :
                algs.add(COSEAlgorithmIdentifier.ES384.getValue());
                break;
            case Algorithm.RS384 :
                algs.add(COSEAlgorithmIdentifier.RS384.getValue());
                break;
            case Algorithm.ES512 :
                algs.add(COSEAlgorithmIdentifier.ES512.getValue());
                break;
            case Algorithm.RS512 :
                algs.add(COSEAlgorithmIdentifier.RS512.getValue());
                break;
            case Algorithm.Ed25519:
                algs.add(COSEAlgorithmIdentifier.EdDSA.getValue());
                break;
            case "RS1" :
                algs.add(COSEAlgorithmIdentifier.RS1.getValue());
                break;
            default:
                // NOP
            }
        }

        return algs;
    }

    private void showInfoAfterWebAuthnApiCreate(RegistrationData response) {
        AttestedCredentialData attestedCredentialData = response.getAttestationObject().getAuthenticatorData().getAttestedCredentialData();
        AttestationStatement attestationStatement = response.getAttestationObject().getAttestationStatement();
        Set<AuthenticatorTransport> transports = response.getTransports();

        logger.debugv("createad key's algorithm = {0}", String.valueOf(attestedCredentialData.getCOSEKey().getAlgorithm().getValue()));
        logger.debugv("aaguid = {0}", attestedCredentialData.getAaguid().toString());
        logger.debugv("attestation format = {0}", attestationStatement.getFormat());

        if (CollectionUtil.isNotEmpty(transports)) {
            logger.debugv("transports = [{0}]", transports.stream()
                    .map(AuthenticatorTransport::getValue)
                    .collect(Collectors.joining(",")));
        }
    }

    private void checkAcceptedAuthenticator(RegistrationData response, WebAuthnPolicy policy) throws Exception {
        String aaguid = response.getAttestationObject().getAuthenticatorData().getAttestedCredentialData().getAaguid().toString();
        List<String> acceptableAaguids = policy.getAcceptableAaguids();
        boolean isAcceptedAuthenticator = false;
        if (acceptableAaguids != null && !acceptableAaguids.isEmpty()) {
            for(String acceptableAaguid : acceptableAaguids) {
                if (aaguid.equals(acceptableAaguid)) {
                    isAcceptedAuthenticator = true;
                    break;
                }
            }
        } else {
            // no accepted authenticators means accepting any kind of authenticator
            isAcceptedAuthenticator = true;
        }
        if (!isAcceptedAuthenticator) {
            throw new WebAuthnException("not acceptable aaguid = " + aaguid);
        }
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        // NOP
    }

    private void setErrorResponse(RequiredActionContext context, final String errorCase, final String errorMessage, @Deprecated final EventType originalEventType) {
        Response errorResponse = null;
        switch (errorCase) {
        case WEBAUTHN_ERROR_REGISTER_VERIFICATION:
            logger.warnv("WebAuthn API .create() response validation failure. {0}", errorMessage);
            EventBuilder registerVerificationEvent = context.getEvent()
                .detail(REG_ERR_LABEL, errorCase)
                .detail(REG_ERR_DETAIL_LABEL, errorMessage);
            EventBuilder deprecatedRegisterVerificationEvent = registerVerificationEvent.clone().event(originalEventType);
            registerVerificationEvent.error(Errors.INVALID_USER_CREDENTIALS);
            deprecatedRegisterVerificationEvent.error(Errors.INVALID_USER_CREDENTIALS);
            errorResponse = context.form()
                .setError(errorCase, errorMessage)
                .setAttribute(WEB_AUTHN_TITLE_ATTR, WEBAUTHN_REGISTER_TITLE)
                .createWebAuthnErrorPage();
            context.challenge(errorResponse);
            break;
        case WEBAUTHN_ERROR_REGISTRATION:
            logger.warn(errorCase);
            EventBuilder registrationEvent = context.getEvent()
                    .detail(REG_ERR_LABEL, errorCase)
                    .detail(REG_ERR_DETAIL_LABEL, errorMessage);
            EventBuilder deprecatedRegistrationEvent = registrationEvent.clone().event(originalEventType);
            deprecatedRegistrationEvent.error(Errors.INVALID_REGISTRATION);
            registrationEvent.error(Errors.INVALID_REGISTRATION);
            errorResponse = context.form()
                .setError(errorCase, errorMessage)
                .setAttribute(WEB_AUTHN_TITLE_ATTR, WEBAUTHN_REGISTER_TITLE)
                .createWebAuthnErrorPage();
            context.challenge(errorResponse);
            break;
        default:
                // NOP
        }
    }

    private boolean isFormDataRequest(HttpRequest request) {
        MediaType mediaType = request.getHttpHeaders().getMediaType();
        return mediaType != null && mediaType.isCompatible(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
    }

}
