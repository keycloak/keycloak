package org.keycloak.models.utils;

import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.PasswordToken;
import org.keycloak.util.Time;

import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CredentialValidation {

    private static int hashIterations(RealmModel realm) {
        PasswordPolicy policy = realm.getPasswordPolicy();
        if (policy != null) {
            return policy.getHashIterations();
        }
        return -1;

    }

    /**
     * Will update password if hash iteration policy has changed
     *
     * @param realm
     * @param user
     * @param password
     * @return
     */
    public static boolean validPassword(RealmModel realm, UserModel user, String password) {
        UserCredentialValueModel passwordCred = null;
        for (UserCredentialValueModel cred : user.getCredentialsDirectly()) {
            if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                passwordCred = cred;
            }
        }
        if (passwordCred == null) return false;

        return validateHashedCredential(realm, user, password, passwordCred);

    }

    public static boolean validateHashedCredential(RealmModel realm, UserModel user, String unhashedCredValue, UserCredentialValueModel credential) {
        boolean validated = new Pbkdf2PasswordEncoder(credential.getSalt()).verify(unhashedCredValue, credential.getValue(), credential.getHashIterations());
        if (validated) {
            int iterations = hashIterations(realm);
            if (iterations > -1 && iterations != credential.getHashIterations()) {
                UserCredentialValueModel newCred = new UserCredentialValueModel();
                newCred.setType(credential.getType());
                newCred.setDevice(credential.getDevice());
                newCred.setSalt(credential.getSalt());
                newCred.setHashIterations(iterations);
                newCred.setValue(new Pbkdf2PasswordEncoder(newCred.getSalt()).encode(unhashedCredValue, iterations));
                user.updateCredentialDirectly(newCred);
            }

        }
        return validated;
    }

    public static boolean validPasswordToken(RealmModel realm, UserModel user, String encodedPasswordToken) {
        JWSInput jws = new JWSInput(encodedPasswordToken);
        if (!RSAProvider.verify(jws, realm.getPublicKey())) {
            return false;
        }
        try {
            PasswordToken passwordToken = jws.readJsonContent(PasswordToken.class);
            if (!passwordToken.getRealm().equals(realm.getName())) {
                return false;
            }
            if (!passwordToken.getUser().equals(user.getId())) {
                return false;
            }
            if (Time.currentTime() - passwordToken.getTimestamp() > realm.getAccessCodeLifespanUserAction()) {
                return false;
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean validHOTP(RealmModel realm, UserModel user, String otp) {
        UserCredentialValueModel passwordCred = null;
        OTPPolicy policy = realm.getOTPPolicy();
        HmacOTP validator = new HmacOTP(policy.getDigits(), policy.getAlgorithm(), policy.getLookAheadWindow());
        for (UserCredentialValueModel cred : user.getCredentialsDirectly()) {
            if (cred.getType().equals(UserCredentialModel.HOTP)) {
                int counter = validator.validateHOTP(otp, cred.getValue(), cred.getCounter());
                if (counter < 0) return false;
                cred.setCounter(counter);
                user.updateCredentialDirectly(cred);
                return true;
            }
        }
        return false;

    }

    public static boolean validOTP(RealmModel realm, String token, String secret) {
        OTPPolicy policy = realm.getOTPPolicy();
        if (policy.getType().equals(UserCredentialModel.TOTP)) {
            TimeBasedOTP validator = new TimeBasedOTP(policy.getAlgorithm(), policy.getDigits(), policy.getPeriod(), policy.getLookAheadWindow());
            return validator.validateTOTP(token, secret.getBytes());
        } else {
            HmacOTP validator = new HmacOTP(policy.getDigits(), policy.getAlgorithm(), policy.getLookAheadWindow());
            int c = validator.validateHOTP(token, secret, policy.getInitialCounter());
            return c > -1;
        }

    }

    public static boolean validTOTP(RealmModel realm, UserModel user, String otp) {
        UserCredentialValueModel passwordCred = null;
        OTPPolicy policy = realm.getOTPPolicy();
        TimeBasedOTP validator = new TimeBasedOTP(policy.getAlgorithm(), policy.getDigits(), policy.getPeriod(), policy.getLookAheadWindow());
        for (UserCredentialValueModel cred : user.getCredentialsDirectly()) {
            if (cred.getType().equals(UserCredentialModel.TOTP)) {
                if (validator.validateTOTP(otp, cred.getValue().getBytes())) {
                    return true;
                }
            }
        }
        return false;

    }
    public static boolean validSecret(RealmModel realm, UserModel user, String secret) {
        for (UserCredentialValueModel cred : user.getCredentialsDirectly()) {
            if (cred.getType().equals(UserCredentialModel.SECRET)) {
                if (cred.getValue().equals(secret)) return true;
            }
        }
        return false;

    }

    /**
     * Must validate all credentials.  FYI, password hashes may be rehashed and updated based on realm hash password policies.
     *
     * @param realm
     * @param user
     * @param credentials
     * @return
     */
    public static boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> credentials) {
        for (UserCredentialModel credential : credentials) {
            if (!validCredential(realm, user, credential)) return false;
        }
        return true;
    }

    /**
     * Must validate all credentials.  FYI, password hashes may be rehashed and updated based on realm hash password policies.
     *
     * @param realm
     * @param user
     * @param credentials
     * @return
     */
    public static boolean validCredentials(RealmModel realm, UserModel user, UserCredentialModel... credentials) {
        for (UserCredentialModel credential : credentials) {
            if (!validCredential(realm, user, credential)) return false;
        }
        return true;
    }

    private static boolean validCredential(RealmModel realm, UserModel user, UserCredentialModel credential) {
        if (credential.getType().equals(UserCredentialModel.PASSWORD)) {
            if (!validPassword(realm, user, credential.getValue())) {
                return false;
            }
        } else if (credential.getType().equals(UserCredentialModel.PASSWORD_TOKEN)) {
            if (!validPasswordToken(realm, user, credential.getValue())) {
                return false;
            }
        } else if (credential.getType().equals(UserCredentialModel.TOTP)) {
            if (!validTOTP(realm, user, credential.getValue())) {
                return false;
            }
        } else if (credential.getType().equals(UserCredentialModel.HOTP)) {
            if (!validHOTP(realm, user, credential.getValue())) {
                return false;
            }
        } else if (credential.getType().equals(UserCredentialModel.SECRET)) {
            if (!validSecret(realm, user, credential.getValue())) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }
}
