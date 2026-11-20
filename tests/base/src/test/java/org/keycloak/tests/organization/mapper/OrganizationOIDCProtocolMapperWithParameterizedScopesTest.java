package org.keycloak.tests.organization.mapper;

import java.util.List;

import jakarta.ws.rs.core.Response.Status;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.common.Profile;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientScopeBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.oauth.ParameterizedScopeBuilder;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@KeycloakIntegrationTest(config = OrganizationOIDCProtocolMapperWithParameterizedScopesTest.ServerConfig.class)
public class OrganizationOIDCProtocolMapperWithParameterizedScopesTest extends AbstractOrganizationTest {

    static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.PARAMETERIZED_SCOPES);
        }
    }

    @InjectOAuthClient
    OAuthClient oauth;

    private OrganizationResource orga;
    private OrganizationResource orgb;

    @BeforeEach
    public void onBefore() {
        realm.admin().organizations().list(null, null)
                .forEach(org -> realm.admin().organizations().get(org.getId()).delete().close());
        realm.admin().users().list().stream()
                .filter(u -> !"admin".equals(u.getUsername()))
                .forEach(u -> realm.admin().users().get(u.getId()).remove());
        createTestClients();

        // a member of multiple organizations only gets the organization claim pinned through the scope
        orga = realm.admin().organizations().get(createOrganization("org-a").getId());
        orgb = realm.admin().organizations().get(createOrganization("org-b").getId());
        addMember(orga);
        UserRepresentation member = getUserRepresentation(memberEmail);
        orgb.members().addMember(member.getId()).close();
        oauth.client("direct-grant", "password");
    }

    @Test
    public void testSpecificOrganizationScope() {
        String orgaAlias = orga.toRepresentation().getAlias();
        assertScopeAndClaims("organization:" + orgaAlias, orgaAlias);
    }

    @Test
    public void testAllOrganizationsScope() {
        assertScopeAndClaims("organization:*", orga.toRepresentation().getAlias(), orgb.toRepresentation().getAlias());
    }

    @Test
    public void testSpecificOrganizationScopeWhenOrganizationIsDefaultClientScope() {
        setOrganizationAsDefaultClientScope();

        // the default organization scope must not be granted alongside the parameterized one
        String orgaAlias = orga.toRepresentation().getAlias();
        assertScopeAndClaims("organization:" + orgaAlias, orgaAlias);
    }

    @Test
    public void testDefaultOrganizationScopeKeptWhenOtherScopeValueContainsOrganization() {
        assertDefaultOrganizationScopeKept(ParameterizedScopeBuilder.create("resource").parameterizedScopeType("string").build(),
                "resource:organization:acme");
    }

    @Test
    public void testDefaultOrganizationScopeKeptWhenOtherScopeNameStartsWithOrganization() {
        assertDefaultOrganizationScopeKept(ClientScopeBuilder.create().name("organization:reports").protocol(OIDCLoginProtocol.LOGIN_PROTOCOL).build(),
                "organization:reports");
    }

    @Test
    public void testDefaultOrganizationScopeKeptWhenParameterizedScopeNameStartsWithOrganization() {
        assertDefaultOrganizationScopeKept(ParameterizedScopeBuilder.create("organization:resource").parameterizedScopeType("string").build(),
                "organization:resource:acme");
    }

    @Test
    public void testOrganizationValueRejectedByParameterizedScopeNotGranted() {
        // a parameterized scope carrying the organization mapper whose type rejects the organization alias
        ProtocolMapperRepresentation orgMapper = realm.admin().clientScopes().get(getOrganizationClientScopeId()).getProtocolMappers().getMappers().stream()
                .filter(m -> OIDCLoginProtocolFactory.ORGANIZATION.equals(m.getName()))
                .findAny()
                .orElseThrow();
        orgMapper.setId(null);
        String orgaAlias = orga.toRepresentation().getAlias();
        addOptionalClientScope(ParameterizedScopeBuilder.create("org-id").parameterizedScopeType("integer").mappers(orgMapper).build());

        oauth.scope("openid org-id:" + orgaAlias);
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        assertThat(Status.fromStatusCode(response.getStatusCode()), is(Status.OK));
        assertThat(response.getScope(), not(containsString("org-id")));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getOtherClaims().keySet(), not(hasItem(OAuth2Constants.ORGANIZATION)));
    }

    @Test
    public void testAllOrganizationsScopeIgnoredWhenOrganizationsDisabled() {
        realm.updateWithCleanup(r -> r.organizationsEnabled(false));

        oauth.scope("openid organization:*");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        assertThat(Status.fromStatusCode(response.getStatusCode()), is(Status.OK));
        assertThat(response.getScope(), not(containsString(OIDCLoginProtocolFactory.ORGANIZATION)));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getOtherClaims().keySet(), not(hasItem(OAuth2Constants.ORGANIZATION)));
    }

    @SuppressWarnings("unchecked")
    private void assertDefaultOrganizationScopeKept(ClientScopeRepresentation otherScope, String requestedScope) {
        setOrganizationAsDefaultClientScope();
        addOptionalClientScope(otherScope);
        // a single organization so that the default organization scope maps it
        orgb.members().member(getUserRepresentation(memberEmail).getId()).delete().close();

        oauth.scope("openid " + requestedScope);
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        assertThat(Status.fromStatusCode(response.getStatusCode()), is(Status.OK));
        assertThat(List.of(response.getScope().split(" ")), hasItem(OIDCLoginProtocolFactory.ORGANIZATION));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat((List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION), containsInAnyOrder(orga.toRepresentation().getAlias()));
    }

    private ClientResource getDirectGrantClient() {
        return realm.admin().clients().get(realm.admin().clients().findByClientId("direct-grant").get(0).getId());
    }

    private String getOrganizationClientScopeId() {
        return realm.admin().clientScopes().findAll().stream()
                .filter(s -> OIDCLoginProtocolFactory.ORGANIZATION.equals(s.getName()))
                .findAny()
                .orElseThrow()
                .getId();
    }

    private void setOrganizationAsDefaultClientScope() {
        ClientResource client = getDirectGrantClient();
        String orgScopeId = getOrganizationClientScopeId();
        client.removeOptionalClientScope(orgScopeId);
        client.addDefaultClientScope(orgScopeId);
        realm.cleanup().add(r -> {
            client.removeDefaultClientScope(orgScopeId);
            client.addOptionalClientScope(orgScopeId);
        });
    }

    private void addOptionalClientScope(ClientScopeRepresentation clientScope) {
        String clientScopeId = ApiUtil.getCreatedId(realm.admin().clientScopes().create(clientScope));
        ClientResource client = getDirectGrantClient();
        client.addOptionalClientScope(clientScopeId);
        realm.cleanup().add(r -> {
            client.removeOptionalClientScope(clientScopeId);
            r.clientScopes().get(clientScopeId).remove();
        });
    }

    private void assertScopeAndClaims(String orgScope, String... expectedAliases) {
        oauth.scope("openid " + orgScope);
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        assertThat(Status.fromStatusCode(response.getStatusCode()), is(Status.OK));
        assertTokens(response, orgScope, expectedAliases);

        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertThat(Status.fromStatusCode(response.getStatusCode()), is(Status.OK));
        assertTokens(response, orgScope, expectedAliases);
    }

    @SuppressWarnings("unchecked")
    private void assertTokens(AccessTokenResponse response, String orgScope, String... expectedAliases) {
        assertThat(List.of(response.getScope().split(" ")), hasItem(orgScope));
        assertThat(List.of(response.getScope().split(" ")), not(hasItem(OIDCLoginProtocolFactory.ORGANIZATION)));

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(List.of(accessToken.getScope().split(" ")), hasItem(orgScope));
        assertThat(List.of(accessToken.getScope().split(" ")), not(hasItem(OIDCLoginProtocolFactory.ORGANIZATION)));
        assertThat((List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION), containsInAnyOrder(expectedAliases));

        IDToken idToken = oauth.verifyIDToken(response.getIdToken());
        assertThat((List<String>) idToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION), containsInAnyOrder(expectedAliases));
    }
}
