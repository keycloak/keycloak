package org.keycloak.tests.authz.services.uma;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessToken.Authorization;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.tests.authz.services.Authz;
import org.keycloak.tests.authz.services.Permission;
import org.keycloak.tests.authz.services.config.DefaultAuthzServicesServerConfig;
import org.keycloak.tests.authz.services.config.DefaultResourceServerConfig;
import org.keycloak.tests.common.BasicUserConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = DefaultAuthzServicesServerConfig.class)
public class UmaGrantTypeTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectUser(config = BasicUserConfig.class)
    ManagedUser user;

    @InjectClient(config = DefaultResourceServerConfig.class, ref = "resource-server-foo")
    ManagedClient rs1;

    @InjectClient(config = DefaultResourceServerConfig.class, ref = "resource-server-bar")
    ManagedClient rs2;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    /**
     * A resource whose URI uses an extension-suffix wildcard must not capture a request for an unrelated path that
     * merely shares the file extension. The matcher used to compare only the part of the pattern from '*' onward,
     * so "/public/*.json" answered for "/admin/secret.json" and the request was evaluated against the public
     * resource's policies instead of the ones guarding /admin.
     */
    @Test
    public void suffixWildcardResourceDoesNotCaptureUnrelatedUri() {
        createResource("public-json", "/public/*.json");
        createResource("admin-all", "/admin/*");
        grantAccessTo("public-json", "admin-all");

        assertResolvesToResource("admin-all", "public-json", "/admin/secret.json");
        assertResolvesToResource("public-json", "admin-all", "/public/report.json");
    }

    /**
     * Enforcing the prefix is not enough on its own: here the suffix pattern's prefix genuinely matches, it is just
     * an ancestor of the resource that actually guards the path. A suffix resource used to outrank a trailing
     * wildcard one structurally, so the broader resource still answered for the narrower one's path.
     */
    @Test
    public void narrowerResourceWinsOverBroaderSuffixWildcardResource() {
        createResource("public-json", "/public/*.json");
        createResource("public-private", "/public/private/*");
        grantAccessTo("public-json", "public-private");

        assertResolvesToResource("public-private", "public-json", "/public/private/secret.json");
    }

    @Test
    public void templatedSuffixWildcardResourceDoesNotCaptureUnrelatedUri() {
        createResource("user-json", "/users/{id}/*.json");
        createResource("api-admin", "/api/admin/*");
        grantAccessTo("user-json", "api-admin");

        assertResolvesToResource("api-admin", "user-json", "/api/admin/secret.json");
    }

    /**
     * The prefix is compared against the normalized URI, so a mutated path cannot be steered onto an unrelated
     * suffix-wildcard resource.
     */
    @Test
    public void mutatedUriCannotBeSteeredOntoSuffixWildcardResource() {
        createResource("public-json", "/public/*.json");
        createResource("admin-all", "/admin/*");
        grantAccessTo("public-json", "admin-all");

        assertResolvesToResource("admin-all", "public-json", "/public/../admin/secret.json");
        assertResolvesToResource("admin-all", "public-json", "/admin;x=1/secret.json");
        assertResolvesToResource("admin-all", "public-json", "/admin//secret.json");
    }

    private void createResource(String name, String uri) {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setName(name);
        resource.setUris(Set.of(uri));

        Authz.create(rs1, resource);
    }

    private void grantAccessTo(String... resourceNames) {
        PolicyRepresentation alwaysGrant = Authz.createAlwaysGrantPolicy(rs1);

        Authz.create(rs1, ResourcePermissionRepresentation.create()
                .name(KeycloakModelUtils.generateId())
                .resources(Set.of(resourceNames))
                .policies(Set.of(alwaysGrant.getId()))
                .build());
    }

    /**
     * Asks for a permission by URI and asserts which resource the server resolved it to. Every resource here is
     * granted by the same always-grant policy, so the assertion is purely about resource identity: a decision-only
     * check could not tell a correct deny from the request failing to resolve at all.
     */
    private void assertResolvesToResource(String expectedResource, String unexpectedResource, String uri) {
        String accessToken = oauth.client(rs1.getClientId(), rs1.getSecret())
                .doPasswordGrantRequest(user.getUsername(), user.getPassword())
                .getAccessToken();
        assertNotNull(accessToken);

        AccessTokenResponse response = oauth.client(rs1.getClientId(), rs1.getSecret())
                .permissionGrantRequest()
                .bearerToken(accessToken)
                .param("permission", uri)
                .param("permission_resource_format", "uri")
                .param("permission_resource_matching_uri", "true")
                .send();

        assertNotNull(response.getAccessToken(),
                "expected an RPT for [" + uri + "] but got error [" + response.getError() + "]");

        Authorization authorization = oauth.parseToken(response.getAccessToken(), AccessToken.class).getAuthorization();
        assertNotNull(authorization, "RPT for [" + uri + "] carries no authorization claim");

        Set<String> resources = authorization.getPermissions().stream()
                .map(org.keycloak.representations.idm.authorization.Permission::getResourceName)
                .collect(Collectors.toSet());

        assertTrue(resources.contains(expectedResource),
                "[" + uri + "] should have resolved to [" + expectedResource + "] but resolved to " + resources);
        assertFalse(resources.contains(unexpectedResource),
                "[" + uri + "] must not resolve to [" + unexpectedResource + "], resolved to " + resources);
    }

    @Test
    public void testIdTokenClaimIssuedDifferentClient() {
        AccessTokenResponse tokenResponse = oauth.client(rs1.getClientId(), rs1.getSecret()).doPasswordGrantRequest(user.getUsername(), user.getPassword());
        String idToken = tokenResponse.getIdToken();
        assertNotNull(idToken);

        Permission.create(rs2)
                .resource("resource")
                .grant();

        tokenResponse = oauth.client(rs2.getClientId(), rs2.getSecret())
                .permissionGrantRequest()
                .claimToken(idToken)
                .send();
        String accessToken = tokenResponse.getAccessToken();
        assertNull(accessToken);
        assertEquals("invalid_claim_token", tokenResponse.getError());
        assertEquals("Token issued to a different client", tokenResponse.getErrorDescription());
    }

    @Test
    public void testExpiredIdTokenClaim() {
        AccessTokenResponse tokenResponse = oauth.client(rs1.getClientId(), rs1.getSecret()).doPasswordGrantRequest(user.getUsername(), user.getPassword());
        String idToken = tokenResponse.getIdToken();
        assertNotNull(idToken);

        Permission.create(rs1)
                .resource("resource")
                .grant();

        timeOffSet.set(Duration.ofDays(1));
        tokenResponse = oauth.client(rs1.getClientId(), rs1.getSecret())
                .permissionGrantRequest()
                .claimToken(idToken)
                .send();
        String accessToken = tokenResponse.getAccessToken();
        assertNull(accessToken);
        assertEquals("invalid_claim_token", tokenResponse.getError());
        assertEquals("Expired token", tokenResponse.getErrorDescription());
    }
}
