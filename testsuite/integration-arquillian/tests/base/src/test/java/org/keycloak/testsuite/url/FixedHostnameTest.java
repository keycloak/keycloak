package org.keycloak.testsuite.url;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.IDPSSODescriptorType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.updaters.Creator;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;

import java.util.stream.Collectors;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_PORT;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SCHEME;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.QUARKUS;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;

@AuthServerContainerExclude({REMOTE, QUARKUS})
public class FixedHostnameTest extends AbstractHostnameTest {

    public static final String SAML_CLIENT_ID = "http://whatever.hostname:8280/app/";

    private String authServerUrl;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation test = RealmBuilder.create().name("test")
                .client(ClientBuilder.create().name("direct-grant").clientId("direct-grant").enabled(true).secret("password").directAccessGrants())
                .user(UserBuilder.create().username("test-user@localhost").password("password"))
                .build();
        testRealms.add(test);

        RealmRepresentation customHostname = RealmBuilder.create().name("hostname")
                .client(ClientBuilder.create().name("direct-grant").clientId("direct-grant").enabled(true).secret("password").directAccessGrants())
                .user(UserBuilder.create().username("test-user@localhost").password("password"))
                .attribute("hostname", "custom-domain.127.0.0.1.nip.io")
                .build();
        testRealms.add(customHostname);
    }

    @Test
    public void fixedHostname() throws Exception {
        authServerUrl = oauth.AUTH_SERVER_ROOT;
        oauth.baseUrl(authServerUrl);

        oauth.clientId("direct-grant");

        try (Keycloak testAdminClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(), getAuthServerContextRoot())) {
            assertWellKnown("test", AUTH_SERVER_SCHEME + "://localhost:" + AUTH_SERVER_PORT);
            assertSamlIdPDescriptor("test", AUTH_SERVER_SCHEME + "://localhost:" + AUTH_SERVER_PORT);

            configureFixed("keycloak.127.0.0.1.nip.io", -1, -1, false);

            assertWellKnown("test", AUTH_SERVER_SCHEME + "://keycloak.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);
            assertSamlIdPDescriptor("test", AUTH_SERVER_SCHEME + "://keycloak.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);
            assertWellKnown("hostname", AUTH_SERVER_SCHEME + "://custom-domain.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);
            assertSamlIdPDescriptor("hostname", AUTH_SERVER_SCHEME + "://custom-domain.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);

            assertTokenIssuer("test", AUTH_SERVER_SCHEME + "://keycloak.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);
            assertTokenIssuer("hostname", AUTH_SERVER_SCHEME + "://custom-domain.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);

            assertInitialAccessTokenFromMasterRealm(testAdminClient,"test", AUTH_SERVER_SCHEME + "://keycloak.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);
            assertSamlLogin(testAdminClient,"test", AUTH_SERVER_SCHEME + "://keycloak.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);
            assertInitialAccessTokenFromMasterRealm(testAdminClient,"hostname", AUTH_SERVER_SCHEME + "://custom-domain.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);
            assertSamlLogin(testAdminClient,"hostname", AUTH_SERVER_SCHEME + "://custom-domain.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);
        } finally {
            reset();
        }
    }

    @Test
    public void fixedHttpPort() throws Exception {
        // Make sure request are always sent with http
        authServerUrl = "http://localhost:8180/auth";
        oauth.baseUrl(authServerUrl);

        oauth.clientId("direct-grant");

        try (Keycloak testAdminClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(), "http://localhost:8180")) {
            assertWellKnown("test", "http://localhost:8180");
            assertSamlIdPDescriptor("test", "http://localhost:8180");

            configureFixed("keycloak.127.0.0.1.nip.io", 80, -1, false);

            assertWellKnown("test", "http://keycloak.127.0.0.1.nip.io");
            assertSamlIdPDescriptor("test", "http://keycloak.127.0.0.1.nip.io");
            assertWellKnown("hostname", "http://custom-domain.127.0.0.1.nip.io");
            assertSamlIdPDescriptor("hostname", "http://custom-domain.127.0.0.1.nip.io");

            assertTokenIssuer("test", "http://keycloak.127.0.0.1.nip.io");
            assertTokenIssuer("hostname", "http://custom-domain.127.0.0.1.nip.io");

            assertInitialAccessTokenFromMasterRealm(testAdminClient,"test", "http://keycloak.127.0.0.1.nip.io");
            assertSamlLogin(testAdminClient,"test", "http://keycloak.127.0.0.1.nip.io");
            assertInitialAccessTokenFromMasterRealm(testAdminClient,"hostname", "http://custom-domain.127.0.0.1.nip.io");
            assertSamlLogin(testAdminClient,"hostname", "http://custom-domain.127.0.0.1.nip.io");
        } finally {
            reset();
        }
    }

    @Test
    public void fixedHostnameAlwaysHttpsHttpsPort() throws Exception {
        // Make sure request are always sent with http
        authServerUrl = "http://localhost:8180/auth";
        oauth.baseUrl(authServerUrl);

        oauth.clientId("direct-grant");

        try (Keycloak testAdminClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(), "http://localhost:8180")) {
            assertWellKnown("test", "http://localhost:8180");
            assertSamlIdPDescriptor("test", "http://localhost:8180");

            configureFixed("keycloak.127.0.0.1.nip.io", -1, 443, true);

            assertWellKnown("test", "https://keycloak.127.0.0.1.nip.io");
            assertSamlIdPDescriptor("test", "https://keycloak.127.0.0.1.nip.io");
            assertWellKnown("hostname", "https://custom-domain.127.0.0.1.nip.io");
            assertSamlIdPDescriptor("hostname", "https://custom-domain.127.0.0.1.nip.io");

            assertTokenIssuer("test", "https://keycloak.127.0.0.1.nip.io");
            assertTokenIssuer("hostname", "https://custom-domain.127.0.0.1.nip.io");

            assertInitialAccessTokenFromMasterRealm(testAdminClient, "test", "https://keycloak.127.0.0.1.nip.io");
            assertSamlLogin(testAdminClient, "test", "https://keycloak.127.0.0.1.nip.io");
            assertInitialAccessTokenFromMasterRealm(testAdminClient, "hostname", "https://custom-domain.127.0.0.1.nip.io");
            assertSamlLogin(testAdminClient, "hostname", "https://custom-domain.127.0.0.1.nip.io");
        } finally {
            reset();
        }
    }

    private void assertInitialAccessTokenFromMasterRealm(Keycloak testAdminClient, String realm, String expectedBaseUrl) throws JWSInputException, ClientRegistrationException {
        ClientInitialAccessCreatePresentation rep = new ClientInitialAccessCreatePresentation();
        rep.setCount(1);
        rep.setExpiration(10000);

        ClientInitialAccessPresentation initialAccess = testAdminClient.realm(realm).clientInitialAccess().create(rep);
        JsonWebToken token = new JWSInput(initialAccess.getToken()).readJsonContent(JsonWebToken.class);
        assertEquals(expectedBaseUrl + "/auth/realms/" + realm, token.getIssuer());

        ClientRegistration clientReg = ClientRegistration.create().url(authServerUrl, realm).build();
        clientReg.auth(Auth.token(initialAccess.getToken()));

        ClientRepresentation client = new ClientRepresentation();
        client.setEnabled(true);
        ClientRepresentation response = clientReg.create(client);

        String registrationAccessToken = response.getRegistrationAccessToken();
        JsonWebToken registrationToken = new JWSInput(registrationAccessToken).readJsonContent(JsonWebToken.class);
        assertEquals(expectedBaseUrl + "/auth/realms/" + realm, registrationToken.getIssuer());
    }

    private void assertTokenIssuer(String realm, String expectedBaseUrl) throws Exception {
        oauth.realm(realm);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");

        AccessToken token = new JWSInput(tokenResponse.getAccessToken()).readJsonContent(AccessToken.class);
        assertEquals(expectedBaseUrl + "/auth/realms/" + realm, token.getIssuer());

        String introspection = oauth.introspectAccessTokenWithClientCredential(oauth.getClientId(), "password", tokenResponse.getAccessToken());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode introspectionNode = objectMapper.readTree(introspection);
        assertTrue(introspectionNode.get("active").asBoolean());
        assertEquals(expectedBaseUrl + "/auth/realms/" + realm, introspectionNode.get("iss").asText());
    }

    private void assertWellKnown(String realm, String expectedBaseUrl) {
        OIDCConfigurationRepresentation config = oauth.doWellKnownRequest(realm);
        assertEquals(expectedBaseUrl + "/auth/realms/" + realm + "/protocol/openid-connect/token", config.getTokenEndpoint());
    }

    private void assertSamlIdPDescriptor(String realm, String expectedBaseUrl) throws Exception {
        final String realmUrl = expectedBaseUrl + "/auth/realms/" + realm;
        final String baseSamlEndpointUrl = realmUrl + "/protocol/saml";
        String entityDescriptor = null;
        try (
          CloseableHttpClient client = HttpClientBuilder.create().build();
          CloseableHttpResponse resp = client.execute(new HttpGet(baseSamlEndpointUrl + "/descriptor"))
          ) {
            entityDescriptor = EntityUtils.toString(resp.getEntity(), GeneralConstants.SAML_CHARSET);
            Object metadataO = SAMLParser.getInstance().parse(new ByteArrayInputStream(entityDescriptor.getBytes(GeneralConstants.SAML_CHARSET)));
            assertThat(metadataO, instanceOf(EntityDescriptorType.class));
            EntityDescriptorType ed = (EntityDescriptorType) metadataO;

            assertThat(ed.getEntityID(), is(realmUrl));

            IDPSSODescriptorType idpDescriptor = ed.getChoiceType().get(0).getDescriptors().get(0).getIdpDescriptor();
            assertThat(idpDescriptor, notNullValue());
            final List<String> locations = idpDescriptor.getSingleSignOnService().stream()
              .map(EndpointType::getLocation)
              .map(URI::toString)
              .collect(Collectors.toList());
            assertThat(locations, Matchers.everyItem(is(baseSamlEndpointUrl)));
        } catch (Exception e) {
            log.errorf("Caught exception while parsing SAML descriptor %s", entityDescriptor);
        }
    }

    private void assertSamlLogin(Keycloak testAdminClient, String realm, String expectedBaseUrl) throws Exception {
        final String realmUrl = expectedBaseUrl + "/auth/realms/" + realm;
        final String baseSamlEndpointUrl = realmUrl + "/protocol/saml";
        String entityDescriptor = null;
        RealmResource realmResource = testAdminClient.realm(realm);
        ClientRepresentation clientRep = ClientBuilder.create()
          .protocol(SamlProtocol.LOGIN_PROTOCOL)
          .clientId(SAML_CLIENT_ID)
          .enabled(true)
          .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false")
          .redirectUris("http://foo.bar/")
          .build();
        try (Creator<ClientResource> c = Creator.create(realmResource, clientRep);
          Creator<UserResource> u = Creator.create(realmResource, UserBuilder.create().username("bicycle").password("race").enabled(true).build())) {
            SAMLDocumentHolder samlResponse = new SamlClientBuilder()
              .authnRequest(new URI(baseSamlEndpointUrl), SAML_CLIENT_ID, "http://foo.bar/", Binding.POST).build()
              .login().user("bicycle", "race").build()
              .getSamlResponse(Binding.POST);

            assertThat(samlResponse.getSamlObject(), org.keycloak.testsuite.util.Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
            ResponseType response = (ResponseType) samlResponse.getSamlObject();

            assertThat(response.getAssertions(), hasSize(1));
            assertThat(response.getAssertions().get(0).getAssertion().getIssuer().getValue(), is(realmUrl));
        } catch (Exception e) {
            log.errorf("Caught exception while parsing SAML descriptor %s", entityDescriptor);
        }
    }

}
