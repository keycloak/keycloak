package org.keycloak.models;

import org.keycloak.models.utils.Pbkdf2PasswordEncoder;

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
    public static boolean validatePassword(RealmModel realm, UserModel user, String password) {
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
}
