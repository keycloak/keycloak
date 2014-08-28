package org.keycloak.models.utils;

import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
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
        boolean validated = false;
        UserCredentialValueModel passwordCred = null;
        for (UserCredentialValueModel cred : user.getCredentialsDirectly()) {
            if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                validated = new Pbkdf2PasswordEncoder(cred.getSalt()).verify(password, cred.getValue(), cred.getHashIterations());
                passwordCred = cred;
            }
        }
        if (validated) {
            int iterations = hashIterations(realm);
            if (iterations > -1 && iterations != passwordCred.getHashIterations()) {
                UserCredentialValueModel newCred = new UserCredentialValueModel();
                newCred.setType(passwordCred.getType());
                newCred.setDevice(passwordCred.getDevice());
                newCred.setSalt(passwordCred.getSalt());
                newCred.setHashIterations(iterations);
                newCred.setValue(new Pbkdf2PasswordEncoder(newCred.getSalt()).encode(password, iterations));
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

    public static boolean validTOTP(RealmModel realm, UserModel user, String otp) {
        UserCredentialValueModel passwordCred = null;
        for (UserCredentialValueModel cred : user.getCredentialsDirectly()) {
            if (cred.getType().equals(UserCredentialModel.TOTP)) {
                if (new TimeBasedOTP().validate(otp, cred.getValue().getBytes())) {
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
