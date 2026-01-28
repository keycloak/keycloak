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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.common.util.Time;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.policy.PolicyError;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import org.jboss.logging.Logger;

import static org.keycloak.credential.PasswordCredentialProviderFactory.METER_ALGORITHM_TAG;
import static org.keycloak.credential.PasswordCredentialProviderFactory.METER_HASHING_STRENGTH_TAG;
import static org.keycloak.credential.PasswordCredentialProviderFactory.METER_REALM_TAG;
import static org.keycloak.credential.PasswordCredentialProviderFactory.METER_VALIDATION_OUTCOME_TAG;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PasswordCredentialProvider implements CredentialProvider<PasswordCredentialModel>, CredentialInputUpdater,
        CredentialInputValidator {

    private static final Logger logger = Logger.getLogger(PasswordCredentialProvider.class);
    private static final String METER_VALIDATION_OUTCOME_VALID_TAG_VALUE = "valid";
    private static final String METER_VALIDATION_OUTCOME_INVALID_TAG_VALUE = "invalid";
    private static final String METER_VALIDATION_OUTCOME_ERROR_TAG_VALUE = "error";


    protected final KeycloakSession session;
    private final Meter.MeterProvider<Counter> meterProvider;
    private final boolean withAlgorithmInMetric;
    private final boolean metricsEnabled;
    private final boolean withRealmInMetric;
    private final boolean withHashingStrengthInMetric;
    private final boolean withOutcomeInMetric;

    public PasswordCredentialProvider(KeycloakSession session, Meter.MeterProvider<Counter> meterProvider, boolean metricsEnabled,
                                      boolean withRealmInMetric, boolean withAlgorithmInMetric, boolean withHashingStrengthInMetric, boolean withOutcomeInMetric) {
        this.session = session;
        this.meterProvider = meterProvider;
        this.metricsEnabled = metricsEnabled;
        this.withRealmInMetric = withRealmInMetric;
        this.withAlgorithmInMetric = withAlgorithmInMetric;
        this.withHashingStrengthInMetric = withHashingStrengthInMetric;
        this.withOutcomeInMetric = withOutcomeInMetric;
    }

    public PasswordCredentialModel getPassword(RealmModel realm, UserModel user) {
        List<CredentialModel> passwords = user.credentialManager().getStoredCredentialsByTypeStream(getType()).collect(Collectors.toList());
        if (passwords.isEmpty()) return null;
        return PasswordCredentialModel.createFromCredentialModel(passwords.get(0));
    }

    public boolean createCredential(RealmModel realm, UserModel user, String password) {
        PasswordPolicy policy = realm.getPasswordPolicy();

        PolicyError error = session.getProvider(PasswordPolicyManagerProvider.class).validate(realm, user, password);
        if (error != null) throw new ModelException(error.getMessage(), error.getParameters());

        PasswordHashProvider hash = getHashProvider(policy);
        if (hash == null) {
            return false;
        }
        try {
            PasswordCredentialModel credentialModel = hash.encodedCredential(password, policy.getHashIterations());
            credentialModel.setCreatedDate(Time.currentTimeMillis());
            createCredential(realm, user, credentialModel);
        } catch (Throwable t) {
            throw new ModelException(t.getMessage(), t);
        }
        return true;
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, PasswordCredentialModel credentialModel) {

        PasswordPolicy policy = realm.getPasswordPolicy();
        int expiredPasswordsPolicyValue = policy.getExpiredPasswords();
        int passwordAgeInDaysPolicy = Math.max(0, policy.getPasswordAgeInDays());

        // 1) create new or reset existing password
        CredentialModel createdCredential;
        CredentialModel oldPassword = getPassword(realm, user);
        if (credentialModel.getCreatedDate() == null) {
            credentialModel.setCreatedDate(Time.currentTimeMillis());
        }
        if (oldPassword == null) { // no password exists --> create new
            createdCredential = user.credentialManager().createStoredCredential(credentialModel);
        } else { // password exists --> update existing
            credentialModel.setId(oldPassword.getId());
            user.credentialManager().updateStoredCredential(credentialModel);
            createdCredential = credentialModel;

            // 2) add a password history item based on the old password
            if (expiredPasswordsPolicyValue > 1 || passwordAgeInDaysPolicy > 0) {
                oldPassword.setId(null);
                oldPassword.setType(PasswordCredentialModel.PASSWORD_HISTORY);
                // Setting the label to "nulL" avoids duplicate label errors
                oldPassword.setUserLabel(null);
                oldPassword = user.credentialManager().createStoredCredential(oldPassword);
            }
        }

        // 3) remove old password history items, if both history policies are set, more restrictive policy wins
        final int passwordHistoryListMaxSize = Math.max(0, expiredPasswordsPolicyValue - 1);

        final long passwordMaxAgeMillis = Time.currentTimeMillis() - Duration.ofDays(passwordAgeInDaysPolicy).toMillis();

        CredentialModel finalOldPassword = oldPassword;
        user.credentialManager().getStoredCredentialsByTypeStream(PasswordCredentialModel.PASSWORD_HISTORY)
                .sorted(CredentialModel.comparingByStartDateDesc())
                .skip(passwordHistoryListMaxSize)
                .filter(credentialModel1 -> !(credentialModel1.getId().equals(finalOldPassword.getId())))
                .filter(credential -> passwordAgePredicate(credential, passwordMaxAgeMillis))
                .collect(Collectors.toList())
                .forEach(p -> user.credentialManager().removeStoredCredentialById(p.getId()));

        return createdCredential;
    }

    private boolean passwordAgePredicate(CredentialModel credential, long passwordMaxAgeMillis) {
        long createdDate = credential.getCreatedDate() == null ? Long.MIN_VALUE : credential.getCreatedDate();
        return createdDate < passwordMaxAgeMillis;
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        return user.credentialManager().removeStoredCredentialById(credentialId);
    }

    @Override
    public PasswordCredentialModel getCredentialFromModel(CredentialModel model) {
        return PasswordCredentialModel.createFromCredentialModel(model);
    }


    protected PasswordHashProvider getHashProvider(PasswordPolicy policy) {
        if (policy != null && policy.getHashAlgorithm() != null) {
            PasswordHashProvider provider = session.getProvider(PasswordHashProvider.class, policy.getHashAlgorithm());
            if (provider != null) {
                return provider;
            } else {
                logger.warnv("Realm PasswordPolicy PasswordHashProvider {0} not found", policy.getHashAlgorithm());
            }
        }

        return session.getProvider(PasswordHashProvider.class);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialType.equals(getType());
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        return createCredential(realm, user, input.getChallengeResponse());
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {

    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        return Stream.empty();
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return getPassword(realm, user) != null;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!(input instanceof UserCredentialModel)) {
            logger.debug("Expected instance of UserCredentialModel for CredentialInput");
            return false;

        }
        if (input.getChallengeResponse() == null) {
            logger.debugv("Input password was null for user {0} ", user.getUsername());
            return false;
        }
        PasswordCredentialModel password = getPassword(realm, user);
        if (password == null) {
            logger.debugv("No password stored for user {0} ", user.getUsername());
            return false;
        }
        String algorithm = password.getPasswordCredentialData().getAlgorithm();
        PasswordHashProvider hash = session.getProvider(PasswordHashProvider.class, algorithm);
        if (hash == null) {
            logger.debugv("PasswordHashProvider {0} not found for user {1} ", algorithm, user.getUsername());
            return false;
        }
        try {
            boolean isValid = hash.verify(input.getChallengeResponse(), password);
            if (!isValid) {
                logger.debugv("Failed password validation for user {0} ", user.getUsername());
                publishMetricIfEnabled(realm, algorithm, hash.credentialHashingStrength(password), METER_VALIDATION_OUTCOME_INVALID_TAG_VALUE);
                return false;
            }

            rehashPasswordIfRequired(session, realm, user, input, password);
        } catch (Throwable t) {
            logger.warn("Error when validating user password", t);
            publishMetricIfEnabled(realm, algorithm, hash.credentialHashingStrength(password), METER_VALIDATION_OUTCOME_ERROR_TAG_VALUE);
            return false;
        }

        publishMetricIfEnabled(realm, algorithm, hash.credentialHashingStrength(password), METER_VALIDATION_OUTCOME_VALID_TAG_VALUE);
        return true;
    }

    private void publishMetricIfEnabled(RealmModel realm, String algorithm, String hashingStrength, String outcome) {
        // Do not publish metrics if metrics are disabled
        if (!metricsEnabled) {
            return;
        }

        List<Tag> tags = new ArrayList<>(5);
        if (withAlgorithmInMetric) {
            tags.add(Tag.of(METER_ALGORITHM_TAG, nullToEmpty(algorithm)));
        }
        if (withHashingStrengthInMetric) {
            tags.add(Tag.of(METER_HASHING_STRENGTH_TAG, nullToEmpty(hashingStrength)));
        }
        if (withRealmInMetric) {
            tags.add(Tag.of(METER_REALM_TAG, nullToEmpty(realm.getName())));
        }
        if (withOutcomeInMetric) {
            tags.add(Tag.of(METER_VALIDATION_OUTCOME_TAG, nullToEmpty(outcome)));
        }

        meterProvider.withTags(tags).increment();

    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void rehashPasswordIfRequired(KeycloakSession session, RealmModel realm, UserModel user, CredentialInput input, PasswordCredentialModel password) {
        PasswordPolicy passwordPolicy = realm.getPasswordPolicy();
        PasswordHashProvider provider;
        if (passwordPolicy != null && passwordPolicy.getHashAlgorithm() != null) {
            provider = session.getProvider(PasswordHashProvider.class, passwordPolicy.getHashAlgorithm());
        } else {
            provider = session.getProvider(PasswordHashProvider.class);
        }

        if (!provider.policyCheck(passwordPolicy, password)) {
            final int iterations = passwordPolicy != null ? passwordPolicy.getHashIterations() : -1;
            final String hashAlgorithm = passwordPolicy != null ? passwordPolicy.getHashAlgorithm() : null;
            // Refresh the password in a different transaction, do not fail if there is a model exception on current modifications due to concurrent logins.
            // Also do not start it as a nested transaction, as the current transaction might have auto-migrated the credential.
            // see: JpaUserCredentialStore#toModel for the on-the-fly migration of the salt column
            session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
                @Override
                protected void commitImpl() {
                    try {
                        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session.getContext(),
                                (KeycloakSession s) -> refreshPassword(s, hashAlgorithm, iterations, input.getChallengeResponse(),
                                        password.getId(), password.getCreatedDate(), password.getUserLabel(), user.getId()));
                    } catch (ModelException e) {
                        logger.info("Error re-hashing the password in a different transaction", e);
                    }
                }

                @Override
                protected void rollbackImpl() {

                }
            });
        }
    }

    private static void refreshPassword(KeycloakSession s, String hashAlgorithm, int iterations, String challenge,
            String passwordId, Long passwordDate, String passwordLabel, String userId) {
        PasswordCredentialModel newPassword = ((hashAlgorithm != null)
                ? s.getProvider(PasswordHashProvider.class, hashAlgorithm)
                : s.getProvider(PasswordHashProvider.class))
                .encodedCredential(challenge, iterations);
        newPassword.setId(passwordId);
        newPassword.setCreatedDate(passwordDate);
        newPassword.setUserLabel(passwordLabel);
        UserModel userModel = s.users().getUserById(s.getContext().getRealm(), userId);
        if (userModel != null) {
            userModel.credentialManager().updateStoredCredential(newPassword);
        }
    }

    @Override
    public String getType() {
        return PasswordCredentialModel.TYPE;
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext) {
        CredentialTypeMetadata.CredentialTypeMetadataBuilder metadataBuilder = CredentialTypeMetadata.builder()
                .type(getType())
                .category(CredentialTypeMetadata.Category.BASIC_AUTHENTICATION)
                .displayName("password-display-name")
                .helpText("password-help-text")
                .iconCssClass("kcAuthenticatorPasswordClass");

        // Check if we are creating or updating password
        UserModel user = metadataContext.getUser();
        if (user != null && user.credentialManager().isConfiguredFor(getType())) {
            metadataBuilder.updateAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        } else {
            metadataBuilder.createAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        }

        return metadataBuilder
                .removeable(false)
                .build(session);
    }
}
