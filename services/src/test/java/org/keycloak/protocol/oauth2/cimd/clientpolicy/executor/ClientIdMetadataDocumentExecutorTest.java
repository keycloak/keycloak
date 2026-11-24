package org.keycloak.protocol.oauth2.cimd.clientpolicy.executor;

import java.net.URI;
import java.util.List;
import java.util.function.BiConsumer;

import org.keycloak.common.Profile;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.resteasy.HttpRequestImpl;
import org.keycloak.services.resteasy.ResteasyKeycloakSession;
import org.keycloak.services.resteasy.ResteasyKeycloakSessionFactory;

import org.jboss.resteasy.mock.MockHttpRequest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ClientIdMetadataDocumentExecutorTest {

    private static final URI CLIENT_ID = URI.create("https://client.example.com/metadata");
    private static final URI REDIRECT_URI = URI.create("https://client.example.com/callback");

    private static KeycloakSession session;

    private ClientIdMetadataDocumentExecutor executor;
    private ClientIdMetadataDocumentExecutor.Configuration configuration;
    private OIDCClientRepresentation clientMetadata;

    @BeforeClass
    public static void beforeClass() throws Exception {
        HttpRequest httpRequest = new HttpRequestImpl(MockHttpRequest.create("GET", URI.create("https://keycloak.org/"), URI.create("https://keycloak.org")));
        Profile.defaults();
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
        ResteasyKeycloakSessionFactory sessionFactory = new ResteasyKeycloakSessionFactory();
        sessionFactory.init();
        session = new ResteasyKeycloakSession(sessionFactory);
        session.getContext().setHttpRequest(httpRequest);
    }

    @Before
    public void before() {
        // neither the http scheme nor a private-use address is allowed by default
        configuration = new ClientIdMetadataDocumentExecutor.Configuration();
        configuration.setTrustedDomains(List.of("client.example.com", "localhost", "127.0.0.1"));
        executor = new ClientIdMetadataDocumentExecutor(session, null);
        executor.setupConfiguration(configuration);

        clientMetadata = new OIDCClientRepresentation();
        clientMetadata.setClientId(CLIENT_ID.toString());
        clientMetadata.setRedirectUris(List.of(REDIRECT_URI.toString()));
    }

    @Test
    public void backchannelLogoutUriIsVerifiedAsServerRequestedUrl() throws Exception {
        assertVerifiedAsServerRequestedUrl(OIDCClientRepresentation::setBackchannelLogoutUri);
    }

    @Test
    public void backchannelClientNotificationEndpointIsVerifiedAsServerRequestedUrl() throws Exception {
        assertVerifiedAsServerRequestedUrl(OIDCClientRepresentation::setBackchannelClientNotificationEndpoint);
    }

    private void assertVerifiedAsServerRequestedUrl(BiConsumer<OIDCClientRepresentation, String> property) throws Exception {
        property.accept(clientMetadata, "http://127.0.0.1:19080/capture");
        assertRejected(AbstractClientIdMetadataDocumentExecutor.ERR_INVALID_SCHEME);

        property.accept(clientMetadata, "https://127.0.0.1:19080/capture");
        assertRejected(AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_ADDRESS);

        // a trusted host that resolves to a loopback address
        property.accept(clientMetadata, "https://localhost/capture");
        assertRejected(AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_ADDRESS);

        configuration.setAllowHttpScheme(true);
        configuration.setAllowPrivateUseAddresses(true);
        property.accept(clientMetadata, "http://127.0.0.1:19080/capture");
        executor.verifyClientMetadata(CLIENT_ID, REDIRECT_URI, clientMetadata);
    }

    private void assertRejected(String errorDetail) {
        ClientPolicyException e = assertThrows(ClientPolicyException.class,
                () -> executor.verifyClientMetadata(CLIENT_ID, REDIRECT_URI, clientMetadata));
        assertEquals(errorDetail, e.getErrorDetail());
    }
}
