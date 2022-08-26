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

package org.keycloak.models.utils;

import org.keycloak.common.Profile;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DefaultRequiredActions {
    public static void addActions(RealmModel realm) {
        if (realm.getRequiredActionProviderByAlias(UserModel.RequiredAction.VERIFY_EMAIL.name()) == null) {
            RequiredActionProviderModel verifyEmail = new RequiredActionProviderModel();
            verifyEmail.setEnabled(true);
            verifyEmail.setAlias(UserModel.RequiredAction.VERIFY_EMAIL.name());
            verifyEmail.setName("Verify Email");
            verifyEmail.setProviderId(UserModel.RequiredAction.VERIFY_EMAIL.name());
            verifyEmail.setDefaultAction(false);
            verifyEmail.setPriority(50);
            realm.addRequiredActionProvider(verifyEmail);

        }

        if (realm.getRequiredActionProviderByAlias(UserModel.RequiredAction.UPDATE_PROFILE.name()) == null) {
            RequiredActionProviderModel updateProfile = new RequiredActionProviderModel();
            updateProfile.setEnabled(true);
            updateProfile.setAlias(UserModel.RequiredAction.UPDATE_PROFILE.name());
            updateProfile.setName("Update Profile");
            updateProfile.setProviderId(UserModel.RequiredAction.UPDATE_PROFILE.name());
            updateProfile.setDefaultAction(false);
            updateProfile.setPriority(40);
            realm.addRequiredActionProvider(updateProfile);
        }

        if (realm.getRequiredActionProviderByAlias(UserModel.RequiredAction.CONFIGURE_TOTP.name()) == null) {
            RequiredActionProviderModel totp = new RequiredActionProviderModel();
            totp.setEnabled(true);
            totp.setAlias(UserModel.RequiredAction.CONFIGURE_TOTP.name());
            totp.setName("Configure OTP");
            totp.setProviderId(UserModel.RequiredAction.CONFIGURE_TOTP.name());
            totp.setDefaultAction(false);
            totp.setPriority(10);
            realm.addRequiredActionProvider(totp);
        }

        if (realm.getRequiredActionProviderByAlias(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name()) == null &&
                Profile.isFeatureEnabled(Profile.Feature.RECOVERY_CODES)) {
            RequiredActionProviderModel recoveryCodes = new RequiredActionProviderModel();
            recoveryCodes.setEnabled(true);
            recoveryCodes.setAlias(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name());
            recoveryCodes.setName("Recovery Authentication Codes");
            recoveryCodes.setProviderId(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name());
            recoveryCodes.setDefaultAction(false);
            recoveryCodes.setPriority(70);
            realm.addRequiredActionProvider(recoveryCodes);
        }

        if (realm.getRequiredActionProviderByAlias(UserModel.RequiredAction.UPDATE_PASSWORD.name()) == null) {
            RequiredActionProviderModel updatePassword = new RequiredActionProviderModel();
            updatePassword.setEnabled(true);
            updatePassword.setAlias(UserModel.RequiredAction.UPDATE_PASSWORD.name());
            updatePassword.setName("Update Password");
            updatePassword.setProviderId(UserModel.RequiredAction.UPDATE_PASSWORD.name());
            updatePassword.setDefaultAction(false);
            updatePassword.setPriority(30);
            realm.addRequiredActionProvider(updatePassword);
        }

        if (realm.getRequiredActionProviderByAlias("terms_and_conditions") == null) {
            RequiredActionProviderModel termsAndConditions = new RequiredActionProviderModel();
            termsAndConditions.setEnabled(false);
            termsAndConditions.setAlias("terms_and_conditions");
            termsAndConditions.setName("Terms and Conditions");
            termsAndConditions.setProviderId("terms_and_conditions");
            termsAndConditions.setDefaultAction(false);
            termsAndConditions.setPriority(20);
            realm.addRequiredActionProvider(termsAndConditions);
        }

        addUpdateLocaleAction(realm);
        addDeleteAccountAction(realm);
        addUpdateEmailAction(realm);
        addWebAuthnRegisterAction(realm);
        addWebAuthnPasswordlessRegisterAction(realm);
    }

    public static void addDeleteAccountAction(RealmModel realm) {
        if (realm.getRequiredActionProviderByAlias("delete_account") == null) {
            RequiredActionProviderModel deleteAccount = new RequiredActionProviderModel();
            deleteAccount.setEnabled(false);
            deleteAccount.setAlias("delete_account");
            deleteAccount.setName("Delete Account");
            deleteAccount.setProviderId("delete_account");
            deleteAccount.setDefaultAction(false);
            deleteAccount.setPriority(60);
            realm.addRequiredActionProvider(deleteAccount);
        }
    }

    public static void addUpdateLocaleAction(RealmModel realm) {
        if (realm.getRequiredActionProviderByAlias("update_user_locale") == null) {
            RequiredActionProviderModel updateUserLocale = new RequiredActionProviderModel();
            updateUserLocale.setEnabled(true);
            updateUserLocale.setAlias("update_user_locale");
            updateUserLocale.setName("Update User Locale");
            updateUserLocale.setProviderId("update_user_locale");
            updateUserLocale.setDefaultAction(false);
            updateUserLocale.setPriority(1000);
            realm.addRequiredActionProvider(updateUserLocale);
        }
    }

    public static void addUpdateEmailAction(RealmModel realm){
        if (realm.getRequiredActionProviderByAlias(UserModel.RequiredAction.UPDATE_EMAIL.name()) == null
                && Profile.isFeatureEnabled(Profile.Feature.UPDATE_EMAIL)){
            RequiredActionProviderModel updateEmail = new RequiredActionProviderModel();
            updateEmail.setEnabled(true);
            updateEmail.setAlias(UserModel.RequiredAction.UPDATE_EMAIL.name());
            updateEmail.setName("Update Email");
            updateEmail.setProviderId(UserModel.RequiredAction.UPDATE_EMAIL.name());
            updateEmail.setDefaultAction(false);
            updateEmail.setPriority(70);
            realm.addRequiredActionProvider(updateEmail);
        }
    }

    public static void addWebAuthnRegisterAction(RealmModel realm) {
        final String PROVIDER_ID = "webauthn-register";

        final boolean isWebAuthnFeatureEnabled = Profile.isFeatureEnabled(Profile.Feature.WEB_AUTHN);
        final boolean isRequiredActionActive = realm.getRequiredActionProviderByAlias(PROVIDER_ID) != null;

        if (isWebAuthnFeatureEnabled && !isRequiredActionActive) {
            final RequiredActionProviderModel webauthnRegister = new RequiredActionProviderModel();
            webauthnRegister.setEnabled(true);
            webauthnRegister.setAlias(PROVIDER_ID);
            webauthnRegister.setName("Webauthn Register");
            webauthnRegister.setProviderId(PROVIDER_ID);
            webauthnRegister.setDefaultAction(false);
            webauthnRegister.setPriority(70);
            realm.addRequiredActionProvider(webauthnRegister);
        }
    }

    public static void addWebAuthnPasswordlessRegisterAction(RealmModel realm) {
        final String PROVIDER_ID = "webauthn-register-passwordless";

        final boolean isWebAuthnFeatureEnabled = Profile.isFeatureEnabled(Profile.Feature.WEB_AUTHN);
        final boolean isRequiredActionActive = realm.getRequiredActionProviderByAlias(PROVIDER_ID) != null;

        if (isWebAuthnFeatureEnabled && !isRequiredActionActive) {
            final RequiredActionProviderModel webauthnRegister = new RequiredActionProviderModel();
            webauthnRegister.setEnabled(true);
            webauthnRegister.setAlias(PROVIDER_ID);
            webauthnRegister.setName("Webauthn Register Passwordless");
            webauthnRegister.setProviderId(PROVIDER_ID);
            webauthnRegister.setDefaultAction(false);
            webauthnRegister.setPriority(80);
            realm.addRequiredActionProvider(webauthnRegister);
        }
    }
}
