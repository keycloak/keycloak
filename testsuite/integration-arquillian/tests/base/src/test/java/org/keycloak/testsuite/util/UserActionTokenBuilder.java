package org.keycloak.testsuite.util;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.authentication.actiontoken.resetcred.ResetCredentialsActionToken;
import org.keycloak.authentication.actiontoken.verifyemail.VerifyEmailActionToken;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 */
public class UserActionTokenBuilder {

    private final Map<String, String> realmAttributes;
    private static final String ATTR_PREFIX = "actionTokenGeneratedByUserLifespan.";

    private UserActionTokenBuilder(HashMap<String, String> attr) {
        realmAttributes = attr;
    }

    public static UserActionTokenBuilder create() {
        return new UserActionTokenBuilder(new HashMap<>());
    }

    public UserActionTokenBuilder resetCredentialsLifespan(int lifespan) {
        realmAttributes.put(ATTR_PREFIX + ResetCredentialsActionToken.TOKEN_TYPE, String.valueOf(lifespan));
        return this;
    }

    public UserActionTokenBuilder verifyEmailLifespan(int lifespan) {
        realmAttributes.put(ATTR_PREFIX + VerifyEmailActionToken.TOKEN_TYPE, String.valueOf(lifespan));
        return this;
    }

    public Map<String, String> build() {
        return realmAttributes;
    }
}
