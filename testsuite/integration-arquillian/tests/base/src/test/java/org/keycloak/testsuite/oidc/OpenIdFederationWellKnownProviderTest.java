package org.keycloak.testsuite.oidc;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.Profile;
import org.keycloak.protocol.oidc.OIDCWellKnownProviderFactory;
import org.keycloak.protocol.oidc.federation.OpenIdFederationWellKnownProviderFactory;
import org.keycloak.representations.idm.OpenIdFederationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.OPMetadata;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.TokenUtil;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class OpenIdFederationWellKnownProviderTest extends AbstractWellKnownProviderTest {

    protected String getWellKnownProviderId() {
        return OpenIdFederationWellKnownProviderFactory.PROVIDER_ID;
    }

    @Test
    public void testOpenIdFederationDiscovery() {
        Client client = AdminClientUtil.createResteasyClient();
        try {
            RealmResource testRealm = adminClient.realm("test");
            RealmRepresentation realmRep = testRealm.toRepresentation();
            realmRep.setOpenIdFederationEnabled(true);
            realmRep.setOpenIdFederationOrganizationName("Keycloak");
            realmRep.setOpenIdFederationResolveEndpoint("https://edugain.org/resolve");
            realmRep.setOpenIdFederationAuthorityHints(Stream.of("https://edugain.org/federation").collect(Collectors.toList()));
            testRealm.update(realmRep);

            OpenIdFederationRepresentation openIdFederationRepresentation = new OpenIdFederationRepresentation();
            openIdFederationRepresentation.setTrustAnchor("https://edugain.org/trust-anchor");
            openIdFederationRepresentation.setClientRegistrationTypesSupported(Stream.of("EXPLICIT").collect(Collectors.toList()));
            openIdFederationRepresentation.setEntityTypes(Stream.of("OPENID_PROVIDER").collect(Collectors.toList()));
            testRealm.openIdFederationsResource().create(openIdFederationRepresentation);

            //When Open Id Federation is configured
            EntityStatement statement = getOIDCFederationDiscoveryRepresentation(client, OAuthClient.AUTH_SERVER_ROOT);
            assertEquals(TokenUtil.ENTITY_STATEMENT_JWT, statement.getType());
            Assert.assertNotNull("Entity Statement can not desirialize", statement);
            String mainUrl = RealmsResource.realmBaseUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)).build("test").toString();
            assertEquals(mainUrl, statement.getIssuer());
            assertEquals(mainUrl, statement.getSubject());
            assertEquals("https://edugain.org/federation", statement.getAuthorityHints().get(0));
            Assert.assertNotNull(statement.getMetadata().getFederationEntity());
            assertEquals("Keycloak", statement.getMetadata().getFederationEntity().getCommonMetadata().getOrganizationName());
            OPMetadata op = statement.getMetadata().getOpenIdProviderMetadata();
            assertEquals(1, op.getClientRegistrationTypes().size());
            assertEquals("explicit", op.getClientRegistrationTypes().get(0));
            assertEquals(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT).path(RealmsResource.class).path(RealmsResource.class, "getOpenIdFederationClientsService").build("test").toString(), op.getFederationRegistrationEndpoint());
            testOidc(op);

            realmRep.setOpenIdFederationEnabled(false);
            testRealm.update(realmRep);
        } finally {
            client.close();
        }
    }

    @Test
    public void testWithoutOpenIdFederationDiscovery() {
        Client client = AdminClientUtil.createResteasyClient();
        try {
            //When no Open Id Federation is configured
            int responseStatus = getOIDCFederationDiscoveryConfiguration(client, OAuthClient.AUTH_SERVER_ROOT).getStatus();
            assertEquals(responseStatus, 404);
        } finally {
            client.close();
        }
    }

    @Test
    @Ignore
    public void testDiscovery() {
    }

    @Test
    @Ignore
    public void testHttpDiscovery() {
    }

    @Test
    @Ignore
    public void testIssuerMatches() throws Exception {
    }

    @Test
    @Ignore
    public void corsTest() {
    }

    @Test
    @Ignore
    public void certs() throws IOException {
    }

    @Test
    @Ignore
    public void certsWithJwks() throws IOException {
    }

    @Test
    @Ignore
    public void testIntrospectionEndpointClaim() throws IOException {
    }

    @Test
    @Ignore
    public void testAcrValuesSupported() throws IOException {
    }

    @Test
    @Ignore
    public void testDpopSigningAlgValuesSupportedWithDpop() throws IOException {
    }

    private EntityStatement getOIDCFederationDiscoveryRepresentation(Client client, String uriTemplate) {
        String jwtFederation = getOIDCFederationDiscoveryConfiguration(client, uriTemplate).readEntity(String.class);
        EntityStatement statement = null;
        statement = oauth.verifyToken(jwtFederation, EntityStatement.class);
        return statement;
    }

    private Response getOIDCFederationDiscoveryConfiguration(Client client, String uriTemplate) {
        UriBuilder builder = UriBuilder.fromUri(uriTemplate);
        URI oidcDiscoveryUri = RealmsResource.wellKnownProviderUrl(builder).build("test", OpenIdFederationWellKnownProviderFactory.PROVIDER_ID);
        WebTarget oidcDiscoveryTarget = client.target(oidcDiscoveryUri);

        return oidcDiscoveryTarget.request().get();
    }


}
