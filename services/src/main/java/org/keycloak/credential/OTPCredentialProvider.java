/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.credential;

import org.jboss.logging.Logger;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.common.util.Time;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.dto.OTPCredentialData;
import org.keycloak.models.credential.dto.OTPSecretData;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.models.utils.TimeBasedOTP;

import java.nio.charset.StandardCharsets;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OTPCredentialProvider implements CredentialProvider<OTPCredentialModel>, CredentialInputValidator/*, OnUserCache*/ {
    private static final Logger logger = Logger.getLogger(OTPCredentialProvider.class);

    protected KeycloakSession session;

    public OTPCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, OTPCredentialModel credentialModel) {
        if (credentialModel.getCreatedDate() == null) {
            credentialModel.setCreatedDate(Time.currentTimeMillis());
        }
        return user.credentialManager().createStoredCredential(credentialModel);
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        return user.credentialManager().removeStoredCredentialById(credentialId);
    }

    @Override
    public OTPCredentialModel getCredentialFromModel(CredentialModel model) {
        return OTPCredentialModel.createFromCredentialModel(model);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return getType().equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) return false;
        return user.credentialManager().getStoredCredentialsByTypeStream(credentialType).findAny().isPresent();
    }

    public boolean isConfiguredFor(RealmModel realm, UserModel user){
        return isConfiguredFor(realm, user, getType());
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        if (!(credentialInput instanceof UserCredentialModel)) {
            logger.debug("Expected instance of UserCredentialModel for CredentialInput");
            return false;

        }
        String challengeResponse = credentialInput.getChallengeResponse();
        if (challengeResponse == null) {
            return false;
        }
        if (ObjectUtil.isBlank(credentialInput.getCredentialId())) {
            logger.debugf("CredentialId is null when validating credential of user %s", user.getUsername());
            return false;
        }

        CredentialModel credential = user.credentialManager().getStoredCredentialById(credentialInput.getCredentialId());
        OTPCredentialModel otpCredentialModel = OTPCredentialModel.createFromCredentialModel(credential);
        OTPSecretData secretData = otpCredentialModel.getOTPSecretData();
        OTPCredentialData credentialData = otpCredentialModel.getOTPCredentialData();
        OTPPolicy policy = realm.getOTPPolicy();
        if (OTPCredentialModel.HOTP.equals(credentialData.getSubType())) {
            HmacOTP validator = new HmacOTP(credentialData.getDigits(), credentialData.getAlgorithm(), policy.getLookAheadWindow());
            int counter = validator.validateHOTP(challengeResponse, secretData.getValue(), credentialData.getCounter());
            if (counter < 0) {
                return false;
            }
            otpCredentialModel.updateCounter(counter);
            user.credentialManager().updateStoredCredential(otpCredentialModel);
            return true;
        } else if (OTPCredentialModel.TOTP.equals(credentialData.getSubType())) {
            TimeBasedOTP validator = new TimeBasedOTP(credentialData.getAlgorithm(), credentialData.getDigits(), credentialData.getPeriod(), policy.getLookAheadWindow());
            return validator.validateTOTP(challengeResponse, secretData.getValue().getBytes(StandardCharsets.UTF_8));
        }
        return false;
    }

    @Override
    public String getType() {
        return OTPCredentialModel.TYPE;
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext) {
        return CredentialTypeMetadata.builder()
                .type(getType())
                .category(CredentialTypeMetadata.Category.TWO_FACTOR)
                .displayName("otp-display-name")
                .helpText("otp-help-text")
                .iconCssClass("kcAuthenticatorOTPClass")
                .createAction(UserModel.RequiredAction.CONFIGURE_TOTP.toString())
                .removeable(true)
                .build(session);
    }
}
