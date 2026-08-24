package org.keycloak.tests.authz;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.client.KeycloakTestingClient;

/**
 * @author mhajas
 */
public abstract class AbstractAuthzTest extends AbstractKeycloakTest {

    @InjectAdminClient
    protected Keycloak adminClient;

    @InjectOAuthClient
    protected OAuthClient oauth;

    @InjectRealm
    protected ManagedRealm managedRealm;

    @Override
    public Keycloak getAdminClient() {
        return adminClient;
    }

    @Override
    public KeycloakTestingClient getTestingClient() {
        if (testingClient == null) {
            String realmSegment = "/realms/" + managedRealm.getName();
            String authServerRoot = managedRealm.getBaseUrl().endsWith(realmSegment)
                    ? managedRealm.getBaseUrl().substring(0, managedRealm.getBaseUrl().length() - realmSegment.length()) + "/auth"
                    : managedRealm.getBaseUrl();
            testingClient = KeycloakTestingClient.getInstance(authServerRoot);
        }
        return testingClient;
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
