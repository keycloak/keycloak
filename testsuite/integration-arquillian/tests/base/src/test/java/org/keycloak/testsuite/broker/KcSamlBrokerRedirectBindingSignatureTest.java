package org.keycloak.testsuite.broker;

import java.net.URI;
import java.security.KeyPair;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.UriUtils;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2LogoutRequestBuilder;
import org.keycloak.saml.SAML2NameIDBuilder;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.util.saml.ModifySamlResponseStepBuilder;

import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.VALIDATE_SIGNATURE;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.getProviderRoot;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * HTTP-Redirect parameter validation and broker signature regressions. With signature validation enabled, the
 * query signature is the only protection ({@code validateSignature=true}, {@code wantAssertionsSigned=false}). An
 * attacker who holds a legitimately signed response for their own account must not be able to get an unsigned
 * response naming a victim processed alongside it under a percent-encoded parameter name ({@code %53AMLResponse}).
 */
public class KcSamlBrokerRedirectBindingSignatureTest extends AbstractInitializedBaseBrokerTest {

    private static final String ASSERTION_NS = JBossSAMLURIConstants.ASSERTION_NSURI.get();

    private static final String VICTIM = "victim";

    private static final String IDP_INITIATED_SSO_URL_NAME = "sales-post";

    private static final String[] ENCODED_RESPONSE_NAMES = {"%53AMLResponse", "SAML%52esponse"};

    private static final boolean[] BOTH = {false, true};

    private KeyPair signingKeyPair;

    private String signingCertificate;

    private String victimId;

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }

    @Before
    public void createLinkedVictim() {
        CryptoIntegration.init(getClass().getClassLoader());
        signingKeyPair = KeyUtils.generateRsaKeyPair(2048);
        signingCertificate = PemUtils.encodeCertificate(
                CertificateUtils.generateV1SelfSignedCertificate(signingKeyPair, "kc-saml-idp"));

        // An existing consumer account linked to the identity provider: a response naming it logs straight in.
        UserRepresentation victim = new UserRepresentation();
        victim.setUsername(VICTIM);
        victim.setEmail("victim@localhost.com");
        victim.setEmailVerified(true);
        victim.setFirstName("Victim");
        victim.setLastName("User");
        victim.setEnabled(true);
        victimId = createUserWithAdminClient(adminClient.realm(bc.consumerRealmName()), victim);

        FederatedIdentityRepresentation link = new FederatedIdentityRepresentation();
        link.setIdentityProvider(bc.getIDPAlias());
        link.setUserId(VICTIM);
        link.setUserName(VICTIM);
        adminClient.realm(bc.consumerRealmName()).users().get(victimId).addFederatedIdentity(bc.getIDPAlias(), link).close();

        configureRedirectSignatureValidation(true);
    }

    @Test
    public void testSignedRedirectLoginIsAccepted() throws Exception {
        sendProviderResponse((endpoint, response, relayState) ->
                endpoint + "?" + signedRedirectQuery(withNameId(response, VICTIM), relayState),
                Response.Status.OK);

        assertThat(victimSessions(), hasSize(1));
    }

    @Test
    public void testEncodedParameterNameAttackIsRejected() throws Exception {
        for (String encodedName : ENCODED_RESPONSE_NAMES) {
            sendProviderResponse((endpoint, response, relayState) ->
                    endpoint + "?" + encodedName + "=" + unsignedRedirectValue(withNameId(copy(response), VICTIM))
                            + "&" + signedRedirectQuery(response, relayState),
                    Response.Status.BAD_REQUEST);

            assertThat(victimSessions(), empty());
        }
    }

    // Unless semicolon is disabled as a query parameter delimiter, Quarkus splits on it while the raw query
    // parsing used for verification does not, so the unsigned response is processed while the signature is not
    // affected. Undertow does not treat semicolon as a delimiter.
    @Test
    @AuthServerContainerExclude(AuthServer.UNDERTOW)
    public void testSemicolonDelimiterAttackIsRejected() throws Exception {
        sendProviderResponse((endpoint, response, relayState) ->
                endpoint + "?x=1;SAMLResponse=" + unsignedRedirectValue(withNameId(copy(response), VICTIM))
                        + "&" + signedRedirectQuery(response, relayState),
                Response.Status.BAD_REQUEST);

        assertThat(victimSessions(), empty());
    }

    @Test
    public void testIdpInitiatedSignedRedirectLoginIsAccepted() throws Exception {
        sendProviderResponseWithoutCookies((endpoint, response, relayState) -> {
            Document unsolicited = withNameId(toUnsolicited(response, idpInitiatedEndpoint()), VICTIM);
            return idpInitiatedEndpoint() + "?" + signedRedirectQuery(unsolicited, null);
        }, Response.Status.OK);

        assertThat(victimSessions(), hasSize(1));
    }

    @Test
    public void testIdpInitiatedEncodedParameterNameAttackIsRejected() throws Exception {
        // The IdP-initiated endpoint needs no existing authentication session, so the attack is a single request.
        for (String encodedName : ENCODED_RESPONSE_NAMES) {
            sendProviderResponseWithoutCookies((endpoint, response, relayState) -> {
                Document attacker = toUnsolicited(response, idpInitiatedEndpoint());
                return idpInitiatedEndpoint() + "?" + encodedName + "=" + unsignedRedirectValue(withNameId(copy(attacker), VICTIM))
                        + "&" + signedRedirectQuery(attacker, null);
            }, Response.Status.BAD_REQUEST);

            assertThat(victimSessions(), empty());
        }
    }

    @Test
    public void testUnsignedRedirectLoginIsAccepted() throws Exception {
        configureRedirectSignatureValidation(false);

        sendProviderResponse((endpoint, response, relayState) ->
                endpoint + "?SAMLResponse=" + unsignedRedirectValue(withNameId(response, VICTIM))
                        + "&RelayState=" + RedirectBindingUtil.urlEncode(relayState),
                Response.Status.OK);

        assertThat(victimSessions(), hasSize(1));
    }

    @Test
    public void testIdpInitiatedUnsignedRedirectLoginIsAccepted() throws Exception {
        configureRedirectSignatureValidation(false);

        sendProviderResponseWithoutCookies((endpoint, response, relayState) -> {
            Document unsolicited = withNameId(toUnsolicited(response, idpInitiatedEndpoint()), VICTIM);
            return idpInitiatedEndpoint() + "?SAMLResponse=" + unsignedRedirectValue(unsolicited);
        }, Response.Status.OK);

        assertThat(victimSessions(), hasSize(1));
    }

    @Test
    public void testUnsignedRedirectRejectsDuplicateParameters() throws Exception {
        configureRedirectSignatureValidation(false);

        for (String duplicate : new String[] {"SAMLResponse", "%53AMLResponse", "SAML%52esponse"}) {
            assertUnsignedResponseDuplicateRejected("SAMLResponse", duplicate);
        }
        for (String name : new String[] {"RelayState", "SigAlg", "Signature"}) {
            for (boolean encoded : BOTH) {
                assertUnsignedResponseDuplicateRejected(name, duplicateName(name, encoded));
            }
        }
    }

    private void assertUnsignedResponseDuplicateRejected(String name, String duplicateName) throws Exception {
        for (boolean idpInitiated : BOTH) {
            RedirectUrlFactory urlFactory = (endpoint, response, relayState) -> {
                Document victim = withNameId(idpInitiated ? toUnsolicited(response, idpInitiatedEndpoint()) : response, VICTIM);
                String message = unsignedRedirectValue(victim);
                String encodedRelayState = idpInitiated ? "relay-state" : RedirectBindingUtil.urlEncode(relayState);
                String value = switch (name) {
                    case "SAMLResponse" -> message;
                    case "RelayState" -> encodedRelayState;
                    default -> "unused";
                };
                String query = "SAMLResponse=" + message + "&RelayState=" + encodedRelayState;
                if (!name.equals("SAMLResponse") && !name.equals("RelayState")) {
                    query += "&" + name + "=" + value;
                }
                return (idpInitiated ? idpInitiatedEndpoint() : endpoint) + "?" + query + "&" + duplicateName + "=" + value;
            };

            if (idpInitiated) {
                sendProviderResponseWithoutCookies(urlFactory, Response.Status.BAD_REQUEST);
            } else {
                sendProviderResponse(urlFactory, Response.Status.BAD_REQUEST);
            }

            assertThat("Duplicate " + duplicateName + " must be rejected even when signature validation is disabled",
                    victimSessions(), empty());
        }
    }

    @Test
    public void testUnsignedAuthnRequestIsAccepted() throws Exception {
        for (boolean encoded : BOTH) {
            String query = duplicateName("SAMLRequest", encoded) + "=" + unsignedAuthnRequest();

            assertThat(getWithoutCookies(idpEndpoint() + "?" + query), is(Response.Status.OK.getStatusCode()));
        }
    }

    @Test
    public void testUnsignedAuthnRequestRejectsDuplicateParameters() throws Exception {
        for (String name : new String[] {"SAMLRequest", "RelayState", "SigAlg", "Signature"}) {
            for (boolean encoded : BOTH) {
                String message = unsignedAuthnRequest();
                String value = name.equals("SAMLRequest") ? message : "unused";
                String query = "SAMLRequest=" + message;
                if (!name.equals("SAMLRequest")) {
                    query += "&" + name + "=" + value;
                }
                query += "&" + duplicateName(name, encoded) + "=" + value;

                assertThat("Duplicate " + name + " must be rejected even when client signatures are not required",
                        getWithoutCookies(idpEndpoint() + "?" + query), is(Response.Status.BAD_REQUEST.getStatusCode()));
            }
        }
    }

    @Test
    public void testRedirectLogoutIsAccepted() throws Exception {
        for (boolean validateSignature : BOTH) {
            for (boolean encoded : BOTH) {
                String query = victimLogoutQuery(validateSignature)
                        .replace("SAMLRequest=", duplicateName("SAMLRequest", encoded) + "=");

                assertThat(getWithoutCookies(brokerEndpoint() + "?" + query), is(Response.Status.OK.getStatusCode()));
                assertThat(victimSessions(), empty());
            }
        }
    }

    @Test
    public void testRedirectLogoutRejectsDuplicateRequest() throws Exception {
        for (boolean validateSignature : BOTH) {
            for (boolean encoded : BOTH) {
                String query = victimLogoutQuery(validateSignature);
                String message = UriUtils.parseQueryParameters(query, false).getFirst("SAMLRequest");
                String duplicate = duplicateName("SAMLRequest", encoded) + "=" + message;

                assertThat(getWithoutCookies(brokerEndpoint() + "?" + duplicate + "&" + query),
                        is(Response.Status.BAD_REQUEST.getStatusCode()));
                assertThat("A rejected LogoutRequest must not terminate the victim session", victimSessions(), hasSize(1));

                adminClient.realm(bc.consumerRealmName()).users().get(victimId).logout();
            }
        }
    }

    // Logs the victim in through the broker with an unsigned POST, then builds a LogoutRequest for the victim.
    private String victimLogoutQuery(boolean validateSignature) throws Exception {
        configureRedirectSignatureValidation(false);
        loginVictimWithPost();
        assertThat(victimSessions(), hasSize(1));

        configureRedirectSignatureValidation(validateSignature);

        Document request = new SAML2LogoutRequestBuilder()
                .destination(brokerEndpoint())
                .issuer(getProviderRoot() + "/auth/realms/" + bc.providerRealmName())
                .nameId(SAML2NameIDBuilder.value(VICTIM).build())
                .buildDocument();
        BaseSAML2BindingBuilder builder = new BaseSAML2BindingBuilder();
        if (validateSignature) {
            builder.signWith(null, signingKeyPair).signatureAlgorithm(SignatureAlgorithm.RSA_SHA256).signDocument();
        }
        return builder.redirectBinding(request).requestURI(brokerEndpoint()).getRawQuery();
    }

    private void loginVictimWithPost() throws Exception {
        SamlClientBuilder builder = loginAtProvider();
        builder.addStepBuilder(new ModifySamlResponseStepBuilder(Binding.POST, builder) {
            @Override
            protected HttpUriRequest createRequest(URI locationUri, String attributeName, String samlDoc, List<NameValuePair> parameters) throws Exception {
                Document victim = withNameId(DocumentUtil.getDocument(samlDoc), VICTIM);
                return super.createRequest(locationUri, attributeName, DocumentUtil.getDocumentAsString(victim), parameters);
            }
        }).build();

        builder.execute(response -> assertThat(response, statusCodeIsHC(Response.Status.OK)));
    }

    @FunctionalInterface
    private interface RedirectUrlFactory {
        String create(URI brokerEndpoint, Document providerResponse, String relayState) throws Exception;
    }

    /**
     * Logs in to the consumer through the identity provider as the provider user, then, instead of posting the
     * provider's response to the broker, sends a GET to the URL built from it in the same browser session.
     */
    private void sendProviderResponse(RedirectUrlFactory urlFactory, Response.Status expectedStatus) throws Exception {
        sendProviderResponse(urlFactory, expectedStatus, RequestConfig.DEFAULT);
    }

    // As above, but without sending the browser session's cookies, as an attacker's single request to the
    // IdP-initiated endpoint would be.
    private void sendProviderResponseWithoutCookies(RedirectUrlFactory urlFactory, Response.Status expectedStatus) throws Exception {
        sendProviderResponse(urlFactory, expectedStatus, RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).build());
    }

    private void sendProviderResponse(RedirectUrlFactory urlFactory, Response.Status expectedStatus, RequestConfig requestConfig) throws Exception {
        SamlClientBuilder builder = loginAtProvider();

        builder.addStepBuilder(new ModifySamlResponseStepBuilder(Binding.POST, builder) {
            @Override
            protected HttpUriRequest createRequest(URI locationUri, String attributeName, String samlDoc, List<NameValuePair> parameters) throws Exception {
                String relayState = parameters.stream()
                        .filter(p -> GeneralConstants.RELAY_STATE.equals(p.getName()))
                        .map(NameValuePair::getValue)
                        .findFirst().orElse(null);
                HttpGet request = new HttpGet(urlFactory.create(locationUri, DocumentUtil.getDocument(samlDoc), relayState));
                request.setConfig(requestConfig);
                return request;
            }
        }).build();

        builder.execute(response -> assertThat(response, statusCodeIsHC(expectedStatus)));
    }

    private SamlClientBuilder loginAtProvider() throws Exception {
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(
                AbstractSamlTest.SAML_CLIENT_ID_SALES_POST, getConsumerRoot() + "/sales-post/saml", null);

        return new SamlClientBuilder()
                .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), SAML2Request.convert(loginRep), Binding.POST).build()
                .login().idp(bc.getIDPAlias()).build()
                .processSamlResponse(Binding.POST).build()
                .login().user(bc.getUserLogin(), bc.getUserPassword()).build();
    }

    private void configureRedirectSignatureValidation(boolean validateSignature) {
        IdentityProviderRepresentation idp = identityProviderResource.toRepresentation();
        idp.getConfig().put(VALIDATE_SIGNATURE, Boolean.toString(validateSignature));
        idp.getConfig().put(WANT_ASSERTIONS_SIGNED, Boolean.FALSE.toString());
        idp.getConfig().put(SIGNING_CERTIFICATE_KEY, signingCertificate);
        identityProviderResource.update(idp);
    }

    private String unsignedAuthnRequest() throws Exception {
        return unsignedRedirectValue(SAML2Request.convert(SamlClient.createLoginRequestDocument(
                AbstractSamlTest.SAML_CLIENT_ID_SALES_POST, getConsumerRoot() + "/sales-post/saml", URI.create(idpEndpoint()))));
    }

    private static String duplicateName(String name, boolean encoded) {
        return encoded ? "%" + Integer.toHexString(name.charAt(0)) + name.substring(1) : name;
    }

    private static int getWithoutCookies(String url) throws Exception {
        try (CloseableHttpClient http = HttpClients.custom()
                .disableCookieManagement()
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build();
             CloseableHttpResponse response = http.execute(new HttpGet(url))) {
            return response.getStatusLine().getStatusCode();
        }
    }

    private String idpEndpoint() {
        return getConsumerRoot() + "/auth/realms/" + bc.consumerRealmName() + "/protocol/saml";
    }

    private String brokerEndpoint() {
        return getConsumerRoot() + "/auth/realms/" + bc.consumerRealmName() + "/broker/" + bc.getIDPAlias() + "/endpoint";
    }

    private String idpInitiatedEndpoint() {
        return brokerEndpoint() + "/clients/" + IDP_INITIATED_SSO_URL_NAME;
    }

    private List<UserSessionRepresentation> victimSessions() {
        return adminClient.realm(bc.consumerRealmName()).users().get(victimId).getUserSessions();
    }

    private static Document copy(Document document) {
        return (Document) document.cloneNode(true);
    }

    private static Document withNameId(Document document, String nameId) {
        document.getElementsByTagNameNS(ASSERTION_NS, "NameID").item(0).setTextContent(nameId);
        return document;
    }

    // Turns the response into an unsolicited (IdP-initiated) one addressed to the given destination.
    private static Document toUnsolicited(Document document, String destination) {
        Element response = document.getDocumentElement();
        response.setAttribute("Destination", destination);
        response.removeAttribute("InResponseTo");
        NodeList data = document.getElementsByTagNameNS(ASSERTION_NS, "SubjectConfirmationData");
        for (int i = 0; i < data.getLength(); i++) {
            Element element = (Element) data.item(i);
            element.setAttribute("Recipient", destination);
            element.removeAttribute("InResponseTo");
        }
        return document;
    }

    private static String unsignedRedirectValue(Document document) throws Exception {
        return RedirectBindingUtil.deflateBase64URLEncode(DocumentUtil.getDocumentAsString(document));
    }

    // The HTTP-Redirect query (SAMLResponse, optional RelayState, SigAlg, Signature) as the identity provider
    // would issue it, signed with the key whose certificate the consumer trusts.
    private String signedRedirectQuery(Document document, String relayState) throws Exception {
        BaseSAML2BindingBuilder builder = new BaseSAML2BindingBuilder()
                .signatureAlgorithm(SignatureAlgorithm.RSA_SHA256)
                .signWith(null, signingKeyPair)
                .signDocument();
        if (relayState != null) {
            builder.relayState(relayState);
        }
        return builder.redirectBinding(document).responseURI("http://localhost/").getRawQuery();
    }
}
