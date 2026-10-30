package org.keycloak.tests.broker.saml;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.saml.SamlPrincipalType;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2LoginResponseBuilder;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.IdentityProviderMapperBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testframework.util.HttpServerUtil;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An external, directory-backed SAML IdP that populates an email-format NameID from the directory email field sends
 * {@code <NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"/>} for every user without an email.
 * The broker must reject such a response instead of using the empty value as the federated identity key.
 */
@KeycloakIntegrationTest
public class SamlBrokerEmptyNameIdTest {

    private static final String IDP_ALIAS = "external-saml-idp";
    private static final String IDP_ENTITY_ID = "https://external-saml-idp.example.org/";
    private static final String IDP_SSO_PATH = "/external-saml-idp/sso";
    private static final String NAME_ID_FORMAT = JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get();
    private static final String PRINCIPAL_ATTRIBUTE = "uid";

    private static final String ALICE_EMAIL = "alice@example.org";

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

    // The subject the external IdP authenticates for the next authentication request it receives
    private final AtomicReference<IdpSubject> idpSubject = new AtomicReference<>();

    @BeforeEach
    public void startExternalIdp() {
        httpServer.createContext(IDP_SSO_PATH, this::authenticate);

        // localhost keeps the IdP same-site with Keycloak, so browsers send the auth session cookie with its POST back
        String ssoUrl = "http://localhost:" + httpServer.getAddress().getPort() + IDP_SSO_PATH;
        realm.updateIdentityProvider(IDP_ALIAS, idp -> idp.getConfig().put(SAMLIdentityProviderConfig.SINGLE_SIGN_ON_SERVICE_URL, ssoUrl));
    }

    @AfterEach
    public void stopExternalIdp() {
        httpServer.removeContext(IDP_SSO_PATH);
    }

    @Test
    public void nameIdIsUsedAsFederatedIdentityKey() {
        // Makes sure the external IdP responses are accepted unless they carry the empty principals used below
        loginThroughIdp(new IdpSubject(ALICE_EMAIL, "Alice", "Smith"));

        assertTrue(oauth.parseLoginResponse().isSuccess());
        List<UserRepresentation> users = usersLinkedToIdp();
        assertEquals(1, users.size());
        assertEquals(ALICE_EMAIL, users.get(0).getEmail());
        List<FederatedIdentityRepresentation> links = realm.admin().users().get(users.get(0).getId()).getFederatedIdentity();
        assertEquals(1, links.size());
        assertEquals(ALICE_EMAIL, links.get(0).getUserId());
    }

    @Test
    public void emptyNameIdIsRejected() {
        loginThroughIdp(new IdpSubject("", "Alice", "Smith"));

        errorPage.assertCurrent();
        assertEquals("Invalid requester", errorPage.getError());
        assertTrue(usersLinkedToIdp().isEmpty());
    }

    @Test
    public void emptyNameIdDoesNotLogInToAccountLinkedToEmptyNameId() {
        // The account an unfixed server links to the empty NameID of whichever user logs in with it first; the user
        // provides the missing email when reviewing the profile during that first login
        String aliceId = ApiUtil.getCreatedId(realm.admin().users().create(UserBuilder.create("alice")
                .email(ALICE_EMAIL)
                .emailVerified(true)
                .firstName("Alice")
                .lastName("Smith")
                .federatedLink(IDP_ALIAS, "", "")
                .build()));

        loginThroughIdp(new IdpSubject("", "Bob", "Jones"));

        errorPage.assertCurrent();
        assertEquals("Invalid requester", errorPage.getError());
        UserResource alice = realm.admin().users().get(aliceId);
        assertTrue(alice.getUserSessions().isEmpty());
        UserRepresentation aliceRep = alice.toRepresentation();
        assertEquals(ALICE_EMAIL, aliceRep.getEmail());
        assertEquals("Alice", aliceRep.getFirstName());
        assertEquals("Smith", aliceRep.getLastName());
    }

    @Test
    public void emptyPrincipalAttributeIsRejected() {
        realm.updateIdentityProvider(IDP_ALIAS, idp -> {
            idp.getConfig().put(SAMLIdentityProviderConfig.PRINCIPAL_TYPE, SamlPrincipalType.ATTRIBUTE.name());
            idp.getConfig().put(SAMLIdentityProviderConfig.PRINCIPAL_ATTRIBUTE, PRINCIPAL_ATTRIBUTE);
        });

        loginThroughIdp(new IdpSubject(ALICE_EMAIL, Map.of(UserModel.FIRST_NAME, "Alice", UserModel.LAST_NAME, "Smith",
                PRINCIPAL_ATTRIBUTE, "")));

        errorPage.assertCurrent();
        assertEquals("Invalid requester", errorPage.getError());
        assertTrue(usersLinkedToIdp().isEmpty());
    }

    private void loginThroughIdp(IdpSubject subject) {
        idpSubject.set(subject);
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
    }

    private List<UserRepresentation> usersLinkedToIdp() {
        return realm.admin().users().search(null, null, null, null, null, IDP_ALIAS, null, null, null, null, null);
    }

    private void authenticate(HttpExchange exchange) {
        Map<String, String> parameters = URLEncodedUtils.parse(exchange.getRequestURI(), StandardCharsets.UTF_8).stream()
                .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
        AuthnRequestType authnRequest = (AuthnRequestType) SAMLRequestParser
                .parseRequestRedirectBinding(parameters.get(GeneralConstants.SAML_REQUEST_KEY)).getSamlObject();
        String assertionConsumerUrl = authnRequest.getAssertionConsumerServiceURL().toString();

        try {
            String form = new BaseSAML2BindingBuilder<>()
                    .relayState(parameters.get(GeneralConstants.RELAY_STATE))
                    .postBinding(createLoginResponse(authnRequest, idpSubject.get()))
                    .getHtmlResponse(assertionConsumerUrl);
            HttpServerUtil.sendResponse(exchange, 200, Map.of("Content-Type", List.of("text/html")), form);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Document createLoginResponse(AuthnRequestType authnRequest, IdpSubject subject) throws Exception {
        SAML2LoginResponseBuilder builder = new SAML2LoginResponseBuilder()
                .requestID(authnRequest.getID())
                .destination(authnRequest.getAssertionConsumerServiceURL().toString())
                .issuer(IDP_ENTITY_ID)
                .requestIssuer(authnRequest.getIssuer().getValue())
                .assertionExpiration(300)
                .subjectExpiration(300)
                .nameIdentifier(NAME_ID_FORMAT, subject.nameId());
        ResponseType response = builder.buildModel();

        AttributeStatementType attributeStatement = new AttributeStatementType();
        subject.attributes().forEach((name, value) -> {
            AttributeType attribute = new AttributeType(name);
            attribute.addAttributeValue(value);
            attributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(attribute));
        });
        response.getAssertions().get(0).getAssertion().addStatement(attributeStatement);

        return builder.buildDocument(response);
    }

    private record IdpSubject(String nameId, Map<String, String> attributes) {

        IdpSubject(String nameId, String firstName, String lastName) {
            this(nameId, Map.of(UserModel.FIRST_NAME, firstName, UserModel.LAST_NAME, lastName));
        }
    }

    public static class ConsumerRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .identityProviders(IdentityProviderBuilder.create()
                            .providerId(SAMLIdentityProviderFactory.PROVIDER_ID)
                            .alias(IDP_ALIAS)
                            // Brokered profiles are kept in sync with the IdP on every login
                            .attribute(IdentityProviderModel.SYNC_MODE, IdentityProviderSyncMode.FORCE.name()))
                    .identityProviderMappers(
                            attributeMapper(UserModel.FIRST_NAME),
                            attributeMapper(UserModel.LAST_NAME));
        }

        private static IdentityProviderMapperBuilder attributeMapper(String attribute) {
            return IdentityProviderMapperBuilder.create()
                    .name(attribute)
                    .identityProviderAlias(IDP_ALIAS)
                    .identityProviderMapper(UserAttributeMapper.PROVIDER_ID)
                    .attribute(IdentityProviderModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.name())
                    .attribute(UserAttributeMapper.ATTRIBUTE_NAME, attribute)
                    .attribute(UserAttributeMapper.USER_ATTRIBUTE, attribute);
        }
    }
}
