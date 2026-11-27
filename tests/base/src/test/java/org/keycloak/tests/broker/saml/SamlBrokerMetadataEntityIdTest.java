package org.keycloak.tests.broker.saml;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2LoginResponseBuilder;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.util.HttpServerUtil;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * An identity provider that downloads its keys from the metadata descriptor URL only uses the keys of the entity
 * whose {@code entityID} is the configured identity provider entity ID. The external IdP publishes the metadata as a
 * single {@code EntityDescriptor} or as an {@code EntitiesDescriptor} that aggregates several entities.
 */
@KeycloakIntegrationTest
public class SamlBrokerMetadataEntityIdTest {

    private static final String IDP_ALIAS = "external-saml-idp";
    private static final String IDP_ENTITY_ID = "https://external-saml-idp.example.org/";
    private static final String OTHER_ENTITY_ID = "https://other-saml-idp.example.org/";
    private static final String IDP_SSO_PATH = "/external-saml-idp/sso";
    private static final String IDP_METADATA_PATH = "/external-saml-idp/metadata";

    private static final String USERNAME = "alice";
    private static final String USER_EMAIL = "alice@example.org";

    @InjectRealm(config = ConsumerRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectHttpServer
    HttpServer httpServer;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    ErrorPage errorPage;

    // The metadata the external IdP publishes
    private final AtomicReference<String> metadata = new AtomicReference<>();
    // The key the external IdP signs the next response with
    private final AtomicReference<SigningKey> signingKey = new AtomicReference<>();

    private SigningKey idpKey;
    private SigningKey otherKey;

    @BeforeEach
    public void startExternalIdp() throws Exception {
        idpKey = new SigningKey("idp-key", KeyUtils.generateRsaKeyPair(2048));
        otherKey = new SigningKey("other-key", KeyUtils.generateRsaKeyPair(2048));
        httpServer.createContext(IDP_SSO_PATH, this::authenticate);
        httpServer.createContext(IDP_METADATA_PATH, exchange -> HttpServerUtil.sendResponse(exchange, 200,
                Map.of("Content-Type", List.of("application/xml")), metadata.get()));

        // localhost keeps the IdP same-site with Keycloak, so browsers send the auth session cookie with its POST back
        String idpUrl = "http://localhost:" + httpServer.getAddress().getPort();
        realm.updateIdentityProvider(IDP_ALIAS, idp -> {
            idp.getConfig().put(SAMLIdentityProviderConfig.SINGLE_SIGN_ON_SERVICE_URL, idpUrl + IDP_SSO_PATH);
            idp.getConfig().put(IdentityProviderModel.METADATA_DESCRIPTOR_URL, idpUrl + IDP_METADATA_PATH);
            idp.getConfig().put(SAMLIdentityProviderConfig.USE_METADATA_DESCRIPTOR_URL, Boolean.TRUE.toString());
        });
    }

    @AfterEach
    public void stopExternalIdp() {
        httpServer.removeContext(IDP_SSO_PATH);
        httpServer.removeContext(IDP_METADATA_PATH);
    }

    @Test
    public void entityDescriptorWithConfiguredEntityIdIsUsed() throws Exception {
        metadata.set(entityDescriptor(IDP_ENTITY_ID, idpKey));

        loginThroughIdp(idpKey);

        assertLoggedIn();
    }

    @Test
    public void entityDescriptorWithDifferentEntityIdIsRejected() throws Exception {
        metadata.set(entityDescriptor(OTHER_ENTITY_ID, idpKey));

        loginThroughIdp(idpKey);

        assertInvalidSignature();
    }

    @Test
    public void configuredEntityIsSelectedInAggregate() throws Exception {
        metadata.set(entitiesDescriptor(entityDescriptor(OTHER_ENTITY_ID, otherKey), entityDescriptor(IDP_ENTITY_ID, idpKey)));

        // The key of the first entity in the aggregate is not a key of the configured entity
        loginThroughIdp(otherKey);
        assertInvalidSignature();

        loginThroughIdp(idpKey);
        assertLoggedIn();
    }

    @Test
    public void aggregateWithoutConfiguredEntityIsRejected() throws Exception {
        // An entity ID that only starts with the configured one is a different entity
        metadata.set(entitiesDescriptor(entityDescriptor(OTHER_ENTITY_ID, otherKey), entityDescriptor(IDP_ENTITY_ID + "idp", idpKey)));

        loginThroughIdp(idpKey);

        assertInvalidSignature();
    }

    @Test
    public void firstEntityIsUsedWhenNoEntityIdIsConfigured() throws Exception {
        // Without an identity provider entity ID there is nothing to match the metadata against
        realm.updateIdentityProvider(IDP_ALIAS, idp -> idp.getConfig().remove(SAMLIdentityProviderConfig.IDP_ENTITY_ID));
        metadata.set(entitiesDescriptor(entityDescriptor(OTHER_ENTITY_ID, otherKey), entityDescriptor(IDP_ENTITY_ID, idpKey)));

        loginThroughIdp(otherKey);

        assertLoggedIn();
    }

    private void loginThroughIdp(SigningKey key) {
        signingKey.set(key);
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
    }

    private void assertLoggedIn() {
        assertThat(oauth.parseLoginResponse().isSuccess(), is(true));
        assertThat(userSessions(), hasSize(1));
    }

    private void assertInvalidSignature() {
        errorPage.assertCurrent();
        assertThat(errorPage.getError(), is("Invalid signature in response from identity provider."));
        assertThat(userSessions(), empty());
    }

    private List<UserSessionRepresentation> userSessions() {
        String userId = realm.admin().users().searchByUsername(USERNAME, true).get(0).getId();
        return realm.admin().users().get(userId).getUserSessions();
    }

    private void authenticate(HttpExchange exchange) {
        Map<String, String> parameters = URLEncodedUtils.parse(exchange.getRequestURI(), StandardCharsets.UTF_8).stream()
                .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
        AuthnRequestType authnRequest = (AuthnRequestType) SAMLRequestParser
                .parseRequestRedirectBinding(parameters.get(GeneralConstants.SAML_REQUEST_KEY)).getSamlObject();
        String assertionConsumerUrl = authnRequest.getAssertionConsumerServiceURL().toString();
        SigningKey key = signingKey.get();

        try {
            String form = new BaseSAML2BindingBuilder<>()
                    .relayState(parameters.get(GeneralConstants.RELAY_STATE))
                    .signatureAlgorithm(SignatureAlgorithm.RSA_SHA256)
                    .signWith(key.name(), key.keyPair())
                    .signAssertions()
                    .signDocument()
                    .postBinding(createLoginResponse(authnRequest))
                    .getHtmlResponse(assertionConsumerUrl);
            HttpServerUtil.sendResponse(exchange, 200, Map.of("Content-Type", List.of("text/html")), form);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Document createLoginResponse(AuthnRequestType authnRequest) throws Exception {
        SAML2LoginResponseBuilder builder = new SAML2LoginResponseBuilder()
                .requestID(authnRequest.getID())
                .destination(authnRequest.getAssertionConsumerServiceURL().toString())
                .issuer(IDP_ENTITY_ID)
                .requestIssuer(authnRequest.getIssuer().getValue())
                .assertionExpiration(300)
                .subjectExpiration(300)
                .nameIdentifier(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get(), USER_EMAIL);

        return builder.buildDocument(builder.buildModel());
    }

    private static String entityDescriptor(String entityId, SigningKey key) throws Exception {
        String certificate = PemUtils.encodeCertificate(CertificateUtils.generateV1SelfSignedCertificate(key.keyPair(), key.name()));
        // No validUntil/cacheDuration: the loaded keys stay in the cache for the whole test
        return "<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" entityID=\"" + entityId + "\">"
                + "<md:IDPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
                + "<md:KeyDescriptor use=\"signing\"><ds:KeyInfo><ds:KeyName>" + key.name() + "</ds:KeyName>"
                + "<ds:X509Data><ds:X509Certificate>" + certificate + "</ds:X509Certificate></ds:X509Data></ds:KeyInfo></md:KeyDescriptor>"
                + "<md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\"" + entityId + "sso\"/>"
                + "</md:IDPSSODescriptor></md:EntityDescriptor>";
    }

    private static String entitiesDescriptor(String... entityDescriptors) {
        return "<md:EntitiesDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\">"
                + String.join("", entityDescriptors)
                + "</md:EntitiesDescriptor>";
    }

    private record SigningKey(String name, KeyPair keyPair) {
    }

    public static class ConsumerRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .identityProviders(IdentityProviderBuilder.create()
                            .providerId(SAMLIdentityProviderFactory.PROVIDER_ID)
                            .alias(IDP_ALIAS)
                            .attribute(SAMLIdentityProviderConfig.IDP_ENTITY_ID, IDP_ENTITY_ID)
                            .attribute(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, Boolean.TRUE.toString()))
                    // The account the external IdP user is already linked to, so that a login needs no further steps
                    .users(UserBuilder.create(USERNAME)
                            .email(USER_EMAIL)
                            .emailVerified(true)
                            .name("Alice", "Smith")
                            .federatedLink(IDP_ALIAS, USER_EMAIL, USER_EMAIL));
        }
    }
}
