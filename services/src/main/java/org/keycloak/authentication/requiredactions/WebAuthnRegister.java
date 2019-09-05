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

import java.util.Base64;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.UriUtils;
import org.keycloak.credential.WebAuthnCredentialModel;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.WebAuthnPolicy;

import com.webauthn4j.data.WebAuthnRegistrationContext;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.statement.AttestationStatement;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.util.exception.WebAuthnException;
import com.webauthn4j.validator.WebAuthnRegistrationContextValidationResponse;
import com.webauthn4j.validator.WebAuthnRegistrationContextValidator;

public class WebAuthnRegister implements RequiredActionProvider {

    private static final Logger logger = Logger.getLogger(WebAuthnRegister.class);
    private KeycloakSession session;

    public WebAuthnRegister(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        UserModel userModel = context.getUser();
        String userid = userModel.getId();
        String username = userModel.getUsername();
        Challenge challenge = new DefaultChallenge();
        String challengeValue = Base64Url.encode(challenge.getValue());
        context.getAuthenticationSession().setAuthNote(WebAuthnConstants.AUTH_CHALLENGE_NOTE, challengeValue);

        // construct parameters for calling WebAuthn API navigator.credential.create()

        // mandatory
        WebAuthnPolicy policy = context.getRealm().getWebAuthnPolicy();
        List<String> signatureAlgorithmsList = policy.getSignatureAlgorithm();
        String signatureAlgorithms = stringifySignatureAlgorithms(signatureAlgorithmsList);
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
        String excludeCredentialIds = avoidSameAuthenticatorRegister == true ? stringifyExcludeCredentialIds(userModel.getAttribute(WebAuthnConstants.PUBKEY_CRED_ID_ATTR)) : "";

        Response form = context.form()
                .setAttribute(WebAuthnConstants.CHALLENGE, challengeValue)
                .setAttribute(WebAuthnConstants.USER_ID, userid)
                .setAttribute(WebAuthnConstants.USER_NAME, username)
                .setAttribute(WebAuthnConstants.RP_ENTITY_NAME, rpEntityName)
                .setAttribute(WebAuthnConstants.SIGNATURE_ALGORITHMS, signatureAlgorithms)
                .setAttribute(WebAuthnConstants.RP_ID, rpId)
                .setAttribute(WebAuthnConstants.ATTESTATION_CONVEYANCE_PREFERENCE, attestationConveyancePreference)
                .setAttribute(WebAuthnConstants.AUTHENTICATOR_ATTACHMENT, authenticatorAttachment)
                .setAttribute(WebAuthnConstants.REQUIRE_RESIDENT_KEY, requireResidentKey)
                .setAttribute(WebAuthnConstants.USER_VERIFICATION_REQUIREMENT, userVerificationRequirement)
                .setAttribute(WebAuthnConstants.CREATE_TIMEOUT, createTimeout)
                .setAttribute(WebAuthnConstants.EXCLUDE_CREDENTIAL_IDS, excludeCredentialIds.toString())
                .createForm("webauthn-register.ftl");
        context.challenge(form);
    }

    @Override
    public void processAction(RequiredActionContext context) {

        MultivaluedMap<String, String> params = context.getHttpRequest().getDecodedFormParameters();

        // receive error from navigator.credentials.create()
        String errorMsgFromWebAuthnApi = params.getFirst(WebAuthnConstants.ERROR);
        if (errorMsgFromWebAuthnApi != null && !errorMsgFromWebAuthnApi.isEmpty()) {
            setErrorResponse(context, ERR_WEBAUTHN_API_CREATE, errorMsgFromWebAuthnApi);
            return;
        }

        WebAuthnPolicy policy = context.getRealm().getWebAuthnPolicy();
        String rpId = policy.getRpId();
        if (rpId == null || rpId.isEmpty()) rpId =  context.getUriInfo().getBaseUri().getHost();
        String label = params.getFirst(WebAuthnConstants.AUTHENTICATOR_LABEL);
        byte[] clientDataJSON = Base64.getUrlDecoder().decode(params.getFirst(WebAuthnConstants.CLIENT_DATA_JSON));
        byte[] attestationObject = Base64.getUrlDecoder().decode(params.getFirst(WebAuthnConstants.ATTESTATION_OBJECT));
        String publicKeyCredentialId = params.getFirst(WebAuthnConstants.PUBLIC_KEY_CREDENTIAL_ID);

        Origin origin = new Origin(UriUtils.getOrigin(context.getUriInfo().getBaseUri()));
        Challenge challenge = new DefaultChallenge(context.getAuthenticationSession().getAuthNote(WebAuthnConstants.AUTH_CHALLENGE_NOTE));
        ServerProperty serverProperty = new ServerProperty(origin, rpId, challenge, null);
        // check User Verification by considering a malicious user might modify the result of calling WebAuthn API
        boolean isUserVerificationRequired = policy.getUserVerificationRequirement().equals(WebAuthnConstants.OPTION_REQUIRED) == true ? true : false;

        try {
            WebAuthnRegistrationContext registrationContext = new WebAuthnRegistrationContext(clientDataJSON, attestationObject, serverProperty, isUserVerificationRequired);
            // NOTE: not yet verify Attestation Statement based on certificates
            WebAuthnRegistrationContextValidator webAuthnRegistrationContextValidator = WebAuthnRegistrationContextValidator.createNonStrictRegistrationContextValidator();
            WebAuthnRegistrationContextValidationResponse response = webAuthnRegistrationContextValidator.validate(registrationContext);

            showInfoAfterWebAuthnApiCreate(response);

            checkAcceptedAuthenticator(response, policy);

            WebAuthnCredentialModel credential = new WebAuthnCredentialModel();

            credential.setAttestedCredentialData(response.getAttestationObject().getAuthenticatorData().getAttestedCredentialData());
            credential.setAttestationStatement(response.getAttestationObject().getAttestationStatement());
            credential.setCount(response.getAttestationObject().getAuthenticatorData().getSignCount());

            this.session.userCredentialManager().updateCredential(context.getRealm(), context.getUser(), credential);

            // store received Credential ID on Registration onto UserModel in order to be used on Authentication
            String aaguid = response.getAttestationObject().getAuthenticatorData().getAttestedCredentialData().getAaguid().toString();
            context.getUser().setSingleAttribute(WebAuthnConstants.PUBKEY_CRED_ID_ATTR, publicKeyCredentialId);
            context.getUser().setSingleAttribute(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, label);
            context.getUser().setSingleAttribute(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR, aaguid);
            logger.infov("WebAuthn Registration successed. publicKeyCredentialId = {0}, publicKeyCredentialLabel = {1}, publicKeyCredentialAAGUID = {2}",publicKeyCredentialId, label, aaguid);

            context.getEvent()
                .detail("public_key_credential_id", publicKeyCredentialId)
                .detail("public_key_credential_label", label)
                .detail("public_key_credential_aaguid", aaguid);
            context.success();
        } catch (WebAuthnException wae) {
            if (logger.isDebugEnabled()) logger.debug(wae.getMessage(), wae);
            setErrorResponse(context, ERR_WEBAUTHN_API_CREATE, wae.getMessage());
            return;
        } catch (Exception e) {
            if (logger.isDebugEnabled()) logger.debug(e.getMessage(), e);
            setErrorResponse(context, ERR_WEBAUTHN_API_CREATE, e.getMessage());
            return;
        }
    }

    private String stringifySignatureAlgorithms(List<String> signatureAlgorithmsList) {
        if (signatureAlgorithmsList == null || signatureAlgorithmsList.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String s : signatureAlgorithmsList) {
            switch (s) {
            case Algorithm.ES256 :
                sb.append(COSEAlgorithmIdentifier.ES256.getValue()).append(",");
                break;
            case Algorithm.RS256 :
                sb.append(COSEAlgorithmIdentifier.RS256.getValue()).append(",");
                break;
            case Algorithm.ES384 :
                sb.append(COSEAlgorithmIdentifier.ES384.getValue()).append(",");
                break;
            case Algorithm.RS384 :
                sb.append(COSEAlgorithmIdentifier.RS384.getValue()).append(",");
                break;
            case Algorithm.ES512 :
                sb.append(COSEAlgorithmIdentifier.ES512.getValue()).append(",");
                break;
            case Algorithm.RS512 :
                sb.append(COSEAlgorithmIdentifier.RS512.getValue()).append(",");
                break;
            case "RS1" :
                sb.append(COSEAlgorithmIdentifier.RS1.getValue()).append(",");
                break;
            default:
                // NOP
            }
        }
        if (sb.lastIndexOf(",") > -1) sb.deleteCharAt(sb.lastIndexOf(","));
        return sb.toString();
    }

    private String stringifyExcludeCredentialIds(List<String> credentialIdsList) {
        if (credentialIdsList == null || credentialIdsList.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String s : credentialIdsList)
            if (s != null && !s.isEmpty()) sb.append(s).append(",");
        if (sb.lastIndexOf(",") > -1) sb.deleteCharAt(sb.lastIndexOf(","));
        return sb.toString();
    }

    private void showInfoAfterWebAuthnApiCreate(WebAuthnRegistrationContextValidationResponse response) {
        AttestedCredentialData attestedCredentialData = response.getAttestationObject().getAuthenticatorData().getAttestedCredentialData();
        AttestationStatement attestationStatement = response.getAttestationObject().getAttestationStatement();
        logger.debugv("createad key's algorithm = {0}", attestedCredentialData.getCredentialPublicKey().getAlgorithm());
        logger.debugv("aaguid = {0}", attestedCredentialData.getAaguid().toString());
        logger.debugv("attestation format = {0}", attestationStatement.getFormat());
    }

    private void checkAcceptedAuthenticator(WebAuthnRegistrationContextValidationResponse response, WebAuthnPolicy policy) throws Exception {
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

    private static final String ERR_LABEL = "web_authn_registration_error";
    private static final String ERR_DETAIL_LABEL = "web_authn_registration_error_detail";
    private static final String ERR_WEBAUTHN_API_CREATE = "Failed to authenticate by the WebAuthn Authenticator";
    private static final String ERR_WEBAUTHN_VERIFICATION_FAIL = "WetAuthn Registration result is invalid.";
    private static final String ERR_REGISTRATION_FAIL = "Failed to register your WebAuthn Authenticator.";

    private void setErrorResponse(RequiredActionContext context, final String errorCase, final String errorMessage) {
        Response errorResponse = null;
        switch (errorCase) {
        case ERR_WEBAUTHN_API_CREATE:
            logger.warnv("error returned from navigator.credentials.create(). {0}", errorMessage);
            context.getEvent()
                .detail(ERR_LABEL, errorCase)
                .detail(ERR_DETAIL_LABEL, errorMessage)
                .error(Errors.NOT_ALLOWED);
            errorResponse = context.form()
                .setError(errorCase)
                .createErrorPage(Response.Status.BAD_REQUEST);
            context.challenge(errorResponse);
            break;
        case ERR_WEBAUTHN_VERIFICATION_FAIL:
            logger.warnv("WebAuthn API .create() response validation failure. {0}", errorMessage);
            context.getEvent()
                .detail(ERR_LABEL, errorCase)
                .detail(ERR_DETAIL_LABEL, errorMessage)
                .error(Errors.INVALID_USER_CREDENTIALS);
            errorResponse = context.form()
                .setError(errorCase)
                .createErrorPage(Response.Status.BAD_REQUEST);
            context.challenge(errorResponse);
            break;
        case ERR_REGISTRATION_FAIL:
            logger.warn(errorCase);
            context.getEvent()
                .detail(ERR_LABEL, errorCase)
                .detail(ERR_DETAIL_LABEL, errorMessage)
                .error(Errors.INVALID_REGISTRATION);
            errorResponse = context.form()
                .setError(errorCase)
                .createErrorPage(Response.Status.BAD_REQUEST);
            context.challenge(errorResponse);
            break;
        default:
                // NOP
        }
    }

}
