package org.keycloak.testsuite.authz;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.ProfileAssume;

import org.junit.BeforeClass;

import static org.keycloak.common.Profile.Feature.AUTHORIZATION;

/**
 * @author mhajas
 */
public abstract class AbstractAuthzTest extends AbstractKeycloakTest {

    @BeforeClass
    public static void enabled() {
        ProfileAssume.assumeFeatureEnabled(AUTHORIZATION);
    }

    protected AccessToken toAccessToken(String rpt) {
        AccessToken accessToken;

        try {
            accessToken = new JWSInput(rpt).readJsonContent(AccessToken.class);
        } catch (JWSInputException cause) {
            throw new RuntimeException("Failed to deserialize RPT", cause);
        }
        return accessToken;
    }

    protected PolicyRepresentation createAlwaysGrantPolicy(AuthorizationResource authorization) {
        PolicyRepresentation policy = new PolicyRepresentation();
        policy.setName(KeycloakModelUtils.generateId());
        policy.setType("always-grant");
        authorization.policies().create(policy).close();
        return policy;
    }

    protected PolicyRepresentation createAlwaysDenyPolicy(AuthorizationResource authorization) {
        PolicyRepresentation policy = new PolicyRepresentation();
        policy.setName(KeycloakModelUtils.generateId());
        policy.setType("always-deny");
        authorization.policies().create(policy).close();
        return policy;
    }

    protected PolicyRepresentation createOnlyOwnerPolicy(AuthorizationResource authorization) {
        PolicyRepresentation onlyOwnerPolicy = new PolicyRepresentation();

        onlyOwnerPolicy.setName(KeycloakModelUtils.generateId());
        onlyOwnerPolicy.setType("allow-resource-owner");

        authorization.policies().create(onlyOwnerPolicy).close();

        return onlyOwnerPolicy;
    }
}
