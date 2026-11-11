package org.keycloak.tests.broker.saml;

import java.net.URI;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.UriUtils;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2LogoutRequestBuilder;
import org.keycloak.saml.SAML2NameIDBuilder;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.broker.AbstractKcSamlBrokerTest;
import org.keycloak.tests.saml.SamlBrowserlessBrokerClient;
import org.keycloak.tests.saml.SamlBrowserlessBrokerClient.IdentityProviderResponse;
import org.keycloak.tests.saml.SamlResponseDocument;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;

import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.SINGLE_LOGOUT_SERVICE_URL;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.VALIDATE_SIGNATURE;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED;
import static org.keycloak.tests.saml.SamlClient.createLoginRequestDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HTTP-Redirect parameter validation and broker signature regressions. With signature validation enabled,
 * the query signature is the only protection ({@code validateSignature=true}, {@code wantAssertionsSigned=false}).
 * An attacker who holds a legitimately signed response for their own account must not be able to get an unsigned
 * response naming a victim processed alongside it under a percent-encoded parameter name ({@code %53AMLResponse}).
 */
@KeycloakIntegrationTest
public class KcSamlBrokerRedirectBindingSignatureTest extends AbstractKcSamlBrokerTest {

    private static final String VICTIM = "victim";

    private static final String IDP_INITIATED_SSO_URL_NAME = "sales-post";

    private KeyPair signingKeyPair;

    private X509Certificate signingCertificate;

    private String victimId;

    @BeforeEach
    void initSigningKeysAndVictim() {
        CryptoIntegration.init(getClass().getClassLoader());
        signingKeyPair = KeyUtils.generateRsaKeyPair(2048);
        signingCertificate = CertificateUtils.generateV1SelfSignedCertificate(signingKeyPair, "kc-saml-idp");

        // An existing consumer account linked to the identity provider: a response naming it logs straight in.
        try (Response response = getConsumerRealm().admin().users().create(UserBuilder.create(VICTIM)
                .enabled(true)
                .email("victim@localhost.com")
                .emailVerified(true)
                .name("Victim", "User")
                .federatedLink(getIdpAlias(), VICTIM, VICTIM)
                .build())) {
            victimId = ApiUtil.getCreatedId(response);
        }
    }

    @Test
    public void testSignedRedirectLoginIsAccepted() throws Exception {
        enableRedirectSignatureValidation();
        try (SamlBrowserlessBrokerClient broker = newBrokerClient()) {
            IdentityProviderResponse response = broker.login(getUserLogin(), getUserPassword());
            response.document().setNameId(VICTIM);

            assertEquals(Response.Status.OK.getStatusCode(),
                    response.submitRedirect(response.document().toSignedRedirectQuery(signingKeyPair, response.relayState())));
        }

        assertEquals(1, victimSessions().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"%53AMLResponse", "SAML%52esponse"})
    public void testEncodedParameterNameAttackIsRejected(String encodedName) throws Exception {
        enableRedirectSignatureValidation();
        try (SamlBrowserlessBrokerClient broker = newBrokerClient()) {
            IdentityProviderResponse response = broker.login(getUserLogin(), getUserPassword());
            SamlResponseDocument attacker = response.document();
            SamlResponseDocument victim = attacker.copy().setNameId(VICTIM);

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                    response.submitRedirect(encodedNameAttack(encodedName, victim, attacker.toSignedRedirectQuery(signingKeyPair, response.relayState()))));
        }

        assertTrue(victimSessions().isEmpty());
    }

    @Test
    public void testIdpInitiatedSignedRedirectLoginIsAccepted() throws Exception {
        enableRedirectSignatureValidation();
        SamlResponseDocument victim;
        try (SamlBrowserlessBrokerClient broker = newBrokerClient()) {
            victim = broker.login(getUserLogin(), getUserPassword()).document()
                    .toUnsolicited(idpInitiatedEndpoint())
                    .setNameId(VICTIM);
        }

        assertEquals(Response.Status.OK.getStatusCode(),
                getWithoutCookies(idpInitiatedEndpoint() + "?" + victim.toSignedRedirectQuery(signingKeyPair, null)));
        assertEquals(1, victimSessions().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"%53AMLResponse", "SAML%52esponse"})
    public void testIdpInitiatedEncodedParameterNameAttackIsRejected(String encodedName) throws Exception {
        enableRedirectSignatureValidation();
        // The IdP-initiated endpoint needs no existing authentication session, so the attack is a single request.
        SamlResponseDocument attacker;
        try (SamlBrowserlessBrokerClient broker = newBrokerClient()) {
            attacker = broker.login(getUserLogin(), getUserPassword()).document()
                    .toUnsolicited(idpInitiatedEndpoint());
        }

        SamlResponseDocument victim = attacker.copy().setNameId(VICTIM);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                getWithoutCookies(idpInitiatedEndpoint() + "?" + encodedNameAttack(encodedName, victim, attacker.toSignedRedirectQuery(signingKeyPair, null))));
        assertTrue(victimSessions().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testUnsignedRedirectLoginIsAccepted(boolean idpInitiated) throws Exception {
        configureRedirectSignatureValidation(false);
        try (SamlBrowserlessBrokerClient broker = newBrokerClient()) {
            IdentityProviderResponse response = broker.login(getUserLogin(), getUserPassword());
            SamlResponseDocument victim = response.document().setNameId(VICTIM);
            int status;
            if (idpInitiated) {
                victim.toUnsolicited(idpInitiatedEndpoint());
                status = getWithoutCookies(idpInitiatedEndpoint() + "?SAMLResponse=" + victim.toRedirectBinding());
            } else {
                status = response.submitRedirect("SAMLResponse=" + victim.toRedirectBinding()
                        + "&RelayState=" + RedirectBindingUtil.urlEncode(response.relayState()));
            }
            assertEquals(Response.Status.OK.getStatusCode(), status);
        }
        assertEquals(1, victimSessions().size());
    }

    @ParameterizedTest
    @MethodSource("duplicateResponseParameters")
    public void testUnsignedRedirectRejectsDuplicateParameters(String name, String duplicateName, boolean idpInitiated) throws Exception {
        configureRedirectSignatureValidation(false);
        try (SamlBrowserlessBrokerClient broker = newBrokerClient()) {
            IdentityProviderResponse response = broker.login(getUserLogin(), getUserPassword());
            SamlResponseDocument victim = response.document().setNameId(VICTIM);
            if (idpInitiated) {
                victim.toUnsolicited(idpInitiatedEndpoint());
            }
            String message = victim.toRedirectBinding();
            String relayState = idpInitiated ? "relay-state" : RedirectBindingUtil.urlEncode(response.relayState());
            String value = switch (name) {
                case "SAMLResponse" -> message;
                case "RelayState" -> relayState;
                default -> "unused";
            };
            String query = "SAMLResponse=" + message + "&RelayState=" + relayState;
            if (!name.equals("SAMLResponse") && !name.equals("RelayState")) {
                query += "&" + name + "=" + value;
            }
            query += "&" + duplicateName + "=" + value;

            int status = idpInitiated
                    ? getWithoutCookies(idpInitiatedEndpoint() + "?" + query)
                    : response.submitRedirect(query);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), status,
                    "Duplicate " + name + " must be rejected even when signature validation is disabled");
        }
        assertTrue(victimSessions().isEmpty());
    }

    private static Stream<Arguments> duplicateResponseParameters() {
        return Stream.of("SAMLResponse", "RelayState", "SigAlg", "Signature")
                .flatMap(name -> {
                    Stream<String> duplicates = name.equals("SAMLResponse")
                            ? Stream.of(name, "%53AMLResponse", "SAML%52esponse")
                            : Stream.of(name, duplicateName(name, true));
                    return duplicates.flatMap(duplicate -> Stream.of(
                            Arguments.of(name, duplicate, false), Arguments.of(name, duplicate, true)));
                });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testUnsignedAuthnRequestIsAccepted(boolean encodedName) throws Exception {
        String query = duplicateName("SAMLRequest", encodedName) + "=" + unsignedAuthnRequest();
        assertEquals(Response.Status.OK.getStatusCode(),
                getWithoutCookies(getConsumerRealm().getBaseUrl() + "/protocol/saml?" + query));
    }

    @ParameterizedTest
    @CsvSource({
            "SAMLRequest, false", "SAMLRequest, true",
            "RelayState, false", "RelayState, true",
            "SigAlg, false", "SigAlg, true",
            "Signature, false", "Signature, true"
    })
    public void testUnsignedAuthnRequestRejectsDuplicateParameters(String name, boolean encodedName) throws Exception {
        String message = unsignedAuthnRequest();
        String value = name.equals("SAMLRequest") ? message : "unused";
        String query = "SAMLRequest=" + message;
        if (!name.equals("SAMLRequest")) {
            query += "&" + name + "=" + value;
        }
        query += "&" + duplicateName(name, encodedName) + "=" + value;

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                getWithoutCookies(getConsumerRealm().getBaseUrl() + "/protocol/saml?" + query),
                "Duplicate " + name + " must be rejected even when client signatures are not required");
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    public void testRedirectLogoutIsAccepted(boolean validateSignature, boolean encodedName) throws Exception {
        String query = victimLogoutQuery(validateSignature);
        query = query.replace("SAMLRequest=", duplicateName("SAMLRequest", encodedName) + "=");

        assertEquals(Response.Status.OK.getStatusCode(), getWithoutCookies(brokerEndpoint() + "?" + query));
        assertTrue(victimSessions().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    public void testRedirectLogoutRejectsDuplicateRequest(boolean validateSignature, boolean encodedName) throws Exception {
        String query = victimLogoutQuery(validateSignature);
        String message = UriUtils.parseQueryParameters(query, false).getFirst("SAMLRequest");
        String duplicate = duplicateName("SAMLRequest", encodedName) + "=" + message;

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                getWithoutCookies(brokerEndpoint() + "?" + duplicate + "&" + query));
        assertEquals(1, victimSessions().size(), "A rejected LogoutRequest must not terminate the victim session");
    }

    private String victimLogoutQuery(boolean validateSignature) throws Exception {
        configureRedirectSignatureValidation(false);
        try (SamlBrowserlessBrokerClient broker = newBrokerClient()) {
            IdentityProviderResponse response = broker.login(getUserLogin(), getUserPassword());
            response.document().setNameId(VICTIM);
            assertEquals(Response.Status.OK.getStatusCode(), response.submit());
        }
        assertEquals(1, victimSessions().size());

        configureRedirectSignatureValidation(validateSignature);
        IdentityProviderResource resource = getConsumerRealm().admin().identityProviders().get(getIdpAlias());
        IdentityProviderRepresentation idp = resource.toRepresentation();
        idp.getConfig().put(SINGLE_LOGOUT_SERVICE_URL, "");
        resource.update(idp);

        Document request = new SAML2LogoutRequestBuilder()
                .destination(brokerEndpoint())
                .issuer(getProviderRealm().getBaseUrl())
                .nameId(SAML2NameIDBuilder.value(VICTIM).build())
                .buildDocument();
        BaseSAML2BindingBuilder<?> binding = new BaseSAML2BindingBuilder<>();
        if (validateSignature) {
            binding.signWith(null, signingKeyPair).signatureAlgorithm(SignatureAlgorithm.RSA_SHA256).signDocument();
        }
        return binding.redirectBinding(request).requestURI(brokerEndpoint()).getRawQuery();
    }

    private String unsignedAuthnRequest() throws Exception {
        String baseUrl = getConsumerRealm().getBaseUrl();
        String serverRoot = baseUrl.substring(0, baseUrl.indexOf("/realms/"));
        return RedirectBindingUtil.deflateBase64URLEncode(DocumentUtil.asString(SAML2Request.convert(
                createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, serverRoot + "/sales-post/saml",
                        URI.create(baseUrl + "/protocol/saml")))));
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

    // Called per test rather than from @BeforeEach, so the inherited browser login tests keep the unsigned setup.
    private void enableRedirectSignatureValidation() {
        configureRedirectSignatureValidation(true);
    }

    private void configureRedirectSignatureValidation(boolean validateSignature) {
        IdentityProviderResource idpResource = getConsumerRealm().admin().identityProviders().get(getIdpAlias());
        IdentityProviderRepresentation idp = idpResource.toRepresentation();
        idp.getConfig().put(VALIDATE_SIGNATURE, Boolean.toString(validateSignature));
        idp.getConfig().put(WANT_ASSERTIONS_SIGNED, Boolean.FALSE.toString());
        idp.getConfig().put(SIGNING_CERTIFICATE_KEY, PemUtils.encodeCertificate(signingCertificate));
        idpResource.update(idp);
    }

    private SamlBrowserlessBrokerClient newBrokerClient() {
        return new SamlBrowserlessBrokerClient(getConsumerRealm(), SAML_CLIENT_ID_SALES_POST, IDP_SAML_ALIAS);
    }

    // The JAX-RS @QueryParam binding decodes parameter names and takes the first occurrence, so the unsigned
    // response sent under the encoded name is the one processed, while the signature covers the attacker's.
    private static String encodedNameAttack(String encodedName, SamlResponseDocument unsigned, String signedQuery) {
        return encodedName + "=" + unsigned.toRedirectBinding() + "&" + signedQuery;
    }

    private String idpInitiatedEndpoint() {
        return brokerEndpoint() + "/clients/" + IDP_INITIATED_SSO_URL_NAME;
    }

    private String brokerEndpoint() {
        return getConsumerRealm().getBaseUrl() + "/broker/" + IDP_SAML_ALIAS + "/endpoint";
    }

    private List<UserSessionRepresentation> victimSessions() {
        return getConsumerRealm().admin().users().get(victimId).getUserSessions();
    }
}
