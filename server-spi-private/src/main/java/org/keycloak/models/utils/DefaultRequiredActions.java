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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.keycloak.common.Profile;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserModel;

import static org.keycloak.common.Profile.isFeatureEnabled;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DefaultRequiredActions {

    /**
     * Check whether the action is the default one used in a realm and is available in the application
     * Often, the default actions can be disabled due to the fact a particular feature is disabled
     *
     * @param action required action
     * @return true if the required action is the default one and is available
     */
    public static boolean isActionAvailable(RequiredActionProviderModel action) {
        if (action == null) return false;
        final Optional<Action> foundAction = Action.findByAlias(action.getAlias());

        return foundAction.isPresent() && foundAction.get().isAvailable();
    }

    /**
     * Add default required actions to the realm
     *
     * @param realm realm
     */
    public static void addActions(RealmModel realm) {
        Arrays.stream(Action.values()).forEach(f -> f.addAction(realm));
    }

    /**
     * Add default required action to the realm
     *
     * @param realm  realm
     * @param action particular required action
     */
    public static void addAction(RealmModel realm, Action action) {
        Optional.ofNullable(action).ifPresent(f -> f.addAction(realm));
    }

    public enum Action {
        VERIFY_EMAIL(UserModel.RequiredAction.VERIFY_EMAIL.name(), DefaultRequiredActions::addVerifyEmailAction),
        UPDATE_PROFILE(UserModel.RequiredAction.UPDATE_PROFILE.name(), DefaultRequiredActions::addUpdateProfileAction),
        CONFIGURE_TOTP(UserModel.RequiredAction.CONFIGURE_TOTP.name(), DefaultRequiredActions::addConfigureTotpAction),
        UPDATE_PASSWORD(UserModel.RequiredAction.UPDATE_PASSWORD.name(), DefaultRequiredActions::addUpdatePasswordAction),
        TERMS_AND_CONDITIONS(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name(), DefaultRequiredActions::addTermsAndConditionsAction),
        DELETE_ACCOUNT("delete_account", DefaultRequiredActions::addDeleteAccountAction),
        DELETE_CREDENTIAL("delete_credential", DefaultRequiredActions::addDeleteCredentialAction),
        UPDATE_USER_LOCALE("update_user_locale", DefaultRequiredActions::addUpdateLocaleAction),
        UPDATE_EMAIL(UserModel.RequiredAction.UPDATE_EMAIL.name(), DefaultRequiredActions::addUpdateEmailAction, () -> isFeatureEnabled(Profile.Feature.UPDATE_EMAIL)),
        CONFIGURE_RECOVERY_AUTHN_CODES(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name(), DefaultRequiredActions::addRecoveryAuthnCodesAction, () -> isFeatureEnabled(Profile.Feature.RECOVERY_CODES)),
        WEBAUTHN_REGISTER("webauthn-register", DefaultRequiredActions::addWebAuthnRegisterAction, () -> isFeatureEnabled(Profile.Feature.WEB_AUTHN)),
        WEBAUTHN_PASSWORDLESS_REGISTER("webauthn-register-passwordless", DefaultRequiredActions::addWebAuthnPasswordlessRegisterAction, () -> isFeatureEnabled(Profile.Feature.WEB_AUTHN)),
        VERIFY_USER_PROFILE(UserModel.RequiredAction.VERIFY_PROFILE.name(), DefaultRequiredActions::addVerifyProfile),
        IDP_LINK_ACCOUNT("idp_link", DefaultRequiredActions::addIdpLink);

        private final String alias;
        private final Consumer<RealmModel> addAction;
        private final Supplier<Boolean> isAvailable;

        Action(String alias, Consumer<RealmModel> addAction, Supplier<Boolean> isAvailable) {
            this.alias = alias;
            this.addAction = addAction;
            this.isAvailable = isAvailable;
        }

        Action(String alias, Consumer<RealmModel> addAction) {
            this(alias, addAction, () -> true);
        }

        public String getAlias() {
            return alias;
        }

        public void addAction(RealmModel realm) {
            addAction.accept(realm);
        }

        public boolean isAvailable() {
            return isAvailable.get();
        }

        public static Optional<Action> findByAlias(String alias) {
            return Arrays.stream(Action.values())
                    .filter(Objects::nonNull)
                    .filter(f -> f.getAlias().equals(alias))
                    .findFirst();
        }
    }

    public static void addVerifyEmailAction(RealmModel realm) {
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
    }

    public static void addUpdateProfileAction(RealmModel realm) {
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
    }

    public static void addConfigureTotpAction(RealmModel realm) {
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
    }

    public static void addUpdatePasswordAction(RealmModel realm) {
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
    }

    public static void addTermsAndConditionsAction(RealmModel realm) {
        if (realm.getRequiredActionProviderByAlias(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name()) == null) {
            RequiredActionProviderModel termsAndConditions = new RequiredActionProviderModel();
            termsAndConditions.setEnabled(false);
            termsAndConditions.setAlias(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name());
            termsAndConditions.setName("Terms and Conditions");
            termsAndConditions.setProviderId(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name());
            termsAndConditions.setDefaultAction(false);
            termsAndConditions.setPriority(20);
            realm.addRequiredActionProvider(termsAndConditions);
        }
    }

    public static void addVerifyProfile(RealmModel realm) {
        if (realm.getRequiredActionProviderByAlias(UserModel.RequiredAction.VERIFY_PROFILE.name()) == null) {
            RequiredActionProviderModel verifyProfile = new RequiredActionProviderModel();
            verifyProfile.setEnabled(true);
            verifyProfile.setAlias(UserModel.RequiredAction.VERIFY_PROFILE.name());
            verifyProfile.setName("Verify Profile");
            verifyProfile.setProviderId(UserModel.RequiredAction.VERIFY_PROFILE.name());
            verifyProfile.setDefaultAction(false);
            verifyProfile.setPriority(100);
            realm.addRequiredActionProvider(verifyProfile);
        }
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

    public static void addDeleteCredentialAction(RealmModel realm) {
        if (realm.getRequiredActionProviderByAlias("delete_credential") == null) {
            RequiredActionProviderModel deleteCredential = new RequiredActionProviderModel();
            deleteCredential.setEnabled(true);
            deleteCredential.setAlias("delete_credential");
            deleteCredential.setName("Delete Credential");
            deleteCredential.setProviderId("delete_credential");
            deleteCredential.setDefaultAction(false);
            deleteCredential.setPriority(110);
            realm.addRequiredActionProvider(deleteCredential);
        }
    }
    
    public static void addIdpLink(RealmModel realm) {
        if (realm.getRequiredActionProviderByAlias("idp_link") == null) {
            RequiredActionProviderModel idpLink = new RequiredActionProviderModel();
            idpLink.setEnabled(true);
            idpLink.setAlias("idp_link");
            idpLink.setName("Linking Identity Provider");
            idpLink.setProviderId("idp_link");
            idpLink.setDefaultAction(false);
            idpLink.setPriority(120);
            realm.addRequiredActionProvider(idpLink);
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

    public static void addUpdateEmailAction(RealmModel realm) {
        final String PROVIDER_ID = UserModel.RequiredAction.UPDATE_EMAIL.name();

        final boolean isAvailable = Action.UPDATE_EMAIL.isAvailable();
        if (!isAvailable) return;

        final RequiredActionProviderModel provider = realm.getRequiredActionProviderByAlias(PROVIDER_ID);
        final boolean isRequiredActionActive = provider != null;

        if (!isRequiredActionActive) {
            RequiredActionProviderModel updateEmail = new RequiredActionProviderModel();
            updateEmail.setEnabled(false);
            updateEmail.setAlias(PROVIDER_ID);
            updateEmail.setName("Update Email");
            updateEmail.setProviderId(PROVIDER_ID);
            updateEmail.setDefaultAction(false);
            updateEmail.setPriority(70);
            realm.addRequiredActionProvider(updateEmail);
        }
    }

    public static void addRecoveryAuthnCodesAction(RealmModel realm) {
        final String PROVIDER_ID = UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name();

        final boolean isAvailable = Action.CONFIGURE_RECOVERY_AUTHN_CODES.isAvailable();
        if (!isAvailable) return;

        final RequiredActionProviderModel provider = realm.getRequiredActionProviderByAlias(PROVIDER_ID);
        final boolean isRequiredActionActive = provider != null;

        if (!isRequiredActionActive) {
            RequiredActionProviderModel recoveryCodes = new RequiredActionProviderModel();
            recoveryCodes.setEnabled(true);
            recoveryCodes.setAlias(PROVIDER_ID);
            recoveryCodes.setName("Recovery Authentication Codes");
            recoveryCodes.setProviderId(PROVIDER_ID);
            recoveryCodes.setDefaultAction(false);
            recoveryCodes.setPriority(130);
            realm.addRequiredActionProvider(recoveryCodes);
        }
    }

    public static void addWebAuthnRegisterAction(RealmModel realm) {
        final String PROVIDER_ID = "webauthn-register";

        final boolean isAvailable = Action.WEBAUTHN_REGISTER.isAvailable();
        if (!isAvailable) return;

        final RequiredActionProviderModel provider = realm.getRequiredActionProviderByAlias(PROVIDER_ID);
        final boolean isRequiredActionActive = provider != null;

        if (!isRequiredActionActive) {
            final RequiredActionProviderModel webauthnRegister = new RequiredActionProviderModel();
            webauthnRegister.setEnabled(true);
            webauthnRegister.setAlias(PROVIDER_ID);
            webauthnRegister.setName("Webauthn Register");
            webauthnRegister.setProviderId(PROVIDER_ID);
            webauthnRegister.setDefaultAction(false);
            webauthnRegister.setPriority(80);
            realm.addRequiredActionProvider(webauthnRegister);
        }
    }

    public static void addWebAuthnPasswordlessRegisterAction(RealmModel realm) {
        final String PROVIDER_ID = "webauthn-register-passwordless";

        final boolean isAvailable = Action.WEBAUTHN_PASSWORDLESS_REGISTER.isAvailable();
        if (!isAvailable) return;

        final RequiredActionProviderModel provider = realm.getRequiredActionProviderByAlias(PROVIDER_ID);
        final boolean isRequiredActionActive = provider != null;

        if (!isRequiredActionActive) {
            final RequiredActionProviderModel webauthnRegister = new RequiredActionProviderModel();
            webauthnRegister.setEnabled(true);
            webauthnRegister.setAlias(PROVIDER_ID);
            webauthnRegister.setName("Webauthn Register Passwordless");
            webauthnRegister.setProviderId(PROVIDER_ID);
            webauthnRegister.setDefaultAction(false);
            webauthnRegister.setPriority(90);
            realm.addRequiredActionProvider(webauthnRegister);
        }
    }

    private static final HashSet<String> REQUIRED_ACTIONS = new HashSet<>();
    static {
        for (UserModel.RequiredAction value : UserModel.RequiredAction.values()) {
            REQUIRED_ACTIONS.add(value.name());
        }
    }

    /**
     * Checks whether given {@code providerId} case insensitively matches any of {@link UserModel.RequiredAction} enum
     * and if yes, it returns the value in correct form.
     * <p/>
     * This is necessary to stay backward compatible with older deployments where not all provider factories had ids
     * in uppercase. This means that storage can contain some values in incorrect letter-case.
     *
     * @param providerId the required actions providerId
     * @return providerId with correct letter-case, or the original value if it doesn't match any
     *         of {@link UserModel.RequiredAction}
     */
    public static String getDefaultRequiredActionCaseInsensitively(String providerId) {
        if (providerId == null) {
            return null;
        }
        String upperCase = providerId.toUpperCase();
        if (REQUIRED_ACTIONS.contains(upperCase)) {
            return upperCase;
        }
        return providerId;
    }
}
