package org.keycloak.representations.docker;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.representations.JsonWebToken;

import java.util.ArrayList;
import java.util.List;

/**
 *  * {
 *    "iss": "auth.docker.com",
 *    "sub": "jlhawn",
 *    "aud": "registry.docker.com",
 *    "exp": 1415387315,
 *    "nbf": 1415387015,
 *    "iat": 1415387015,
 *    "jti": "tYJCO1c6cnyy7kAn0c7rKPgbV1H1bFws",
 *    "access": [
 *        {
 *        "type": "repository",
 *        "name": "samalba/my-app",
 *        "actions": [
 *           "push"
 *         ]
 *        }
 *    ]
 * }
 */
public class DockerResponseToken extends JsonWebToken {

    @JsonProperty("access")
    protected List<DockerAccess> accessItems = new ArrayList<>();

    public List<DockerAccess> getAccessItems() {
        return accessItems;
    }

    @Override
    public DockerResponseToken id(final String id) {
        super.id(id);
        return this;
    }

    @Override
    public DockerResponseToken expiration(final int expiration) {
        super.expiration(expiration);
        return this;
    }

    @Override
    public DockerResponseToken notBefore(final int notBefore) {
        super.notBefore(notBefore);
        return this;
    }

    @Override
    public DockerResponseToken issuedNow() {
        super.issuedNow();
        return this;
    }

    @Override
    public DockerResponseToken issuedAt(final int issuedAt) {
        super.issuedAt(issuedAt);
        return this;
    }

    @Override
    public DockerResponseToken issuer(final String issuer) {
        super.issuer(issuer);
        return this;
    }

    @Override
    public DockerResponseToken audience(final String... audience) {
        super.audience(audience);
        return this;
    }

    @Override
    public DockerResponseToken subject(final String subject) {
        super.subject(subject);
        return this;
    }

    @Override
    public DockerResponseToken type(final String type) {
        super.type(type);
        return this;
    }

    @Override
    public DockerResponseToken issuedFor(final String issuedFor) {
        super.issuedFor(issuedFor);
        return this;
    }
}
