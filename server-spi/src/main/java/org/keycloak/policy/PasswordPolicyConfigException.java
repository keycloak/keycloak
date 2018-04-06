package org.keycloak.policy;

import org.keycloak.models.ModelException;

/**
 * Created by st on 23/05/17.
 */
public class PasswordPolicyConfigException extends ModelException {

    public PasswordPolicyConfigException(String message) {
        super(message);
    }

}
