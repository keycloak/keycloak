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

import java.util.concurrent.TimeUnit;

import org.keycloak.authentication.authenticators.browser.TrustedDeviceConstants;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.common.util.Time;
import org.keycloak.cookie.CookieProvider;
import org.keycloak.cookie.CookieType;
import org.keycloak.device.DeviceRepresentationProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.TrustedDeviceCredentialInputModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.TrustedDeviceCredentialModel;
import org.keycloak.representations.account.DeviceRepresentation;

import org.jboss.logging.Logger;

/**
 * @author Norbert Kelemen
 * @version $Revision: 1 $
 */
public class TrustedDeviceCredentialProvider implements CredentialProvider<TrustedDeviceCredentialModel>, CredentialInputValidator {
    private static final Logger logger = Logger.getLogger(TrustedDeviceCredentialProvider.class);
    private final KeycloakSession session;

    public TrustedDeviceCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getType() {
        return TrustedDeviceCredentialModel.TYPE;
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, TrustedDeviceCredentialModel credentialModel) {

        if (credentialModel.getCreatedDate() == null) {
            credentialModel.setCreatedDate(Time.currentTimeMillis());
        }

        return user.credentialManager().createStoredCredential(credentialModel);
    }

    public void createTrustedDeviceCredential(RealmModel realm, UserModel user) {

        // Should not happen, but here is a safety check before creating the new credential
        if (getIsDisabled(realm)) {
            logger.warn("Skipping trusted device creation: Trusted device policy is disabled");
            return;
        }

        DeviceRepresentation device = session.getProvider(DeviceRepresentationProvider.class).deviceRepresentation();
        SubjectCredentialManager credentialManager = user.credentialManager();

        // Clean-up expired Trusted Device credentials
        deleteExpiredCredentials(realm, credentialManager);

        // Creating new credential
        String secret = TrustedDeviceCredentialModel.generateSecret();
        var credential = TrustedDeviceCredentialModel.create(device, secret);
        credentialManager.createStoredCredential(credential);

        // Move new credential to first place
        // This is a workaround, because the auth flows' order during the authentication process is based on the
        // order of the user's stored credentials
        credentialManager.moveStoredCredentialTo(credential.getId(), null);

        // Set cookie
        setTrustedDeviceCookie(realm, user, credential, secret);
    }

    public void rotateSecret(RealmModel realm, TrustedDeviceCredentialModel credential, UserModel user) {
        SubjectCredentialManager credentialManager = user.credentialManager();

        // Generate new secret and update the credential
        String newSecret = credential.rotateSecret();
        credentialManager.updateStoredCredential(credential);

        // Keep Cookie's expiration the same
        setTrustedDeviceCookie(realm, user, credential, newSecret);
    }

    private void setTrustedDeviceCookie(RealmModel realm, UserModel user, CredentialModel credential, String secret) {
        CookieProvider cookieProvider = session.getProvider(CookieProvider.class);
        CookieType cookieType = CookieType.getTrustedDeviceCookie(user.getId());

        // Calculate cookie expiration
        long expiresAtMillis = credential.getCreatedDate() + getExpirationMillis(realm);
        long cookieLifetimeMillis = expiresAtMillis - Time.currentTimeMillis();
        int cookieExpirationSeconds = Math.toIntExact(TimeUnit.MILLISECONDS.toSeconds(cookieLifetimeMillis));

        String content = credential.getId() + ':' + secret;

        cookieProvider.set(cookieType, content, cookieExpirationSeconds);
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        return user.credentialManager().removeStoredCredentialById(credentialId);
    }

    public void deleteExpiredCredentials(RealmModel realm, SubjectCredentialManager credentialManager) {
        long expirationLimit = Time.currentTimeMillis() - getExpirationMillis(realm);

        credentialManager.getStoredCredentialsByTypeStream(TrustedDeviceCredentialModel.TYPE)
                .filter(c -> c.getCreatedDate() <= expirationLimit)
                .forEach(c -> credentialManager.removeStoredCredentialById(c.getId()));
    }

    @Override
    public TrustedDeviceCredentialModel getCredentialFromModel(CredentialModel model) {
        return TrustedDeviceCredentialModel.createFromCredentialModel(model);
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext) {
        return CredentialTypeMetadata.builder()
                .type(getType())
                .category(CredentialTypeMetadata.Category.TWO_FACTOR)
                .displayName("trusted-device-display-name")
                .helpText("trusted-device-help-text")
                .iconCssClass("kcAuthenticatorWebAuthnClass")
                .removeable(true)
                .build(session);
    }

    @Override
    public boolean supportsCredentialType(String type) {
        return type.equals(TrustedDeviceCredentialModel.TYPE);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) return false;
        return user.credentialManager().getStoredCredentialsByTypeStream(credentialType).findAny().isPresent();
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        if (!(credentialInput instanceof TrustedDeviceCredentialInputModel)) {
            logger.debug("Expected instance of TrustedDeviceCredentialInputModel for CredentialInput");
            return false;
        }

        if (ObjectUtil.isBlank(credentialInput.getCredentialId())) {
            logger.debugf("CredentialId is null when validating credential of user %s", user.getUsername());
            return false;
        }

        String challengeSecret = credentialInput.getChallengeResponse();
        if (challengeSecret == null || challengeSecret.isEmpty())
            return false;

        // Check whether trusted device policy is disabled
        if (getIsDisabled(realm))
            return false;

        // Get saved user credential
        CredentialModel credential = user.credentialManager().getStoredCredentialById(credentialInput.getCredentialId());

        if (credential == null)
            return false;

        TrustedDeviceCredentialModel tdCredential = TrustedDeviceCredentialModel.createFromCredentialModel(credential);

        long credentialExpiresAt = tdCredential.getCreatedDate() + getExpirationMillis(realm);

        if (credentialExpiresAt <= Time.currentTimeMillis()) {
            // Remove the credential if expired
            deleteCredential(realm, user, credentialInput.getCredentialId());
            return false;
        }

        if (!tdCredential.verifySecret(challengeSecret)) {
            // Remove invalid credential
            deleteCredential(realm, user, credentialInput.getCredentialId());
            return false;
        }

        // Rotate the secret
        rotateSecret(realm, tdCredential, user);
        return true;
    }

    private static boolean getIsDisabled(RealmModel realm) {
        return !realm.getAttribute(TrustedDeviceConstants.REALM_IS_ENABLED_ATTR, false);
    }

    private static long getExpirationMillis(RealmModel realm) {
        int expirationSeconds = realm.getAttribute(
                TrustedDeviceConstants.REALM_EXPIRATION_ATTR,
                TrustedDeviceConstants.DEFAULT_EXPIRATION
        );
        return TimeUnit.SECONDS.toMillis(expirationSeconds);
    }

}
