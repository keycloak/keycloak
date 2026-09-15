package org.keycloak.testframework.realm;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.authentication.actiontoken.resetcred.ResetCredentialsActionToken;
import org.keycloak.authentication.actiontoken.verifyemail.VerifyEmailActionToken;

import static org.keycloak.models.jpa.entities.RealmAttributes.ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN;

public class RealmAttributesBuilder {

    private final Map<String, String> attributes;

    private RealmAttributesBuilder(HashMap<String, String> attributes) {
        this.attributes = attributes;
    }

    public static RealmAttributesBuilder create() {
        return new RealmAttributesBuilder(new HashMap<>());
    }

    public RealmAttributesBuilder resetCredentialsLifespan(int lifespan) {
        attributes.put(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "." + ResetCredentialsActionToken.TOKEN_TYPE, String.valueOf(lifespan));
        return this;
    }

    public RealmAttributesBuilder verifyEmailLifespan(int lifespan) {
        attributes.put(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "." + VerifyEmailActionToken.TOKEN_TYPE, String.valueOf(lifespan));
        return this;
    }

    public Map<String, String> build() {
        return attributes;
    }

}
