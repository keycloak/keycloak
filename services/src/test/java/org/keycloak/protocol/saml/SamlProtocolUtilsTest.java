package org.keycloak.protocol.saml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.common.VerificationException;
import org.keycloak.common.util.UriUtils;
import org.keycloak.dom.saml.v2.protocol.ArtifactResponseType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.rotation.HardcodedKeyLocator;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2LoginResponseBuilder;
import org.keycloak.saml.SAML2LogoutRequestBuilder;
import org.keycloak.saml.SAML2LogoutResponseBuilder;
import org.keycloak.saml.SAML2NameIDBuilder;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLResponseWriter;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.w3c.dom.Document;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThrows;

public class SamlProtocolUtilsTest {

    private final byte[] bytes = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    private static final String RELAY_STATE = "relay-state";

    private static final KeyPair rsaKeyPair;

    static {
        try {
            KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
            rsa.initialize(2048);
            rsaKeyPair = rsa.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void testBuildArtifactResponse() throws ConfigurationException, ProcessingException, ParsingException {

        ResponseType response = new SAML2LoginResponseBuilder()
                .requestID(IDGenerator.create("ID_"))
                .destination("http://localhost:8180/auth/realms/demo/broker/saml-broker/endpoint")
                .issuer("http://saml.idp/saml")
                .assertionExpiration(1000000)
                .subjectExpiration(1000000)
                .requestIssuer("http://localhost:8180/auth/realms/demo")
                .nameIdentifier(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get(), "a@b.c")
                .authMethod(JBossSAMLURIConstants.AC_UNSPECIFIED.get())
                .sessionIndex("idp:" + UUID.randomUUID())
                .buildModel();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        SAMLResponseWriter writer = new SAMLResponseWriter(StaxUtil.getXMLStreamWriter(bos));
        writer.write(response);
        Document responseDoc = DocumentUtil.getDocument(new ByteArrayInputStream(bos.toByteArray()));

        ArtifactResponseType artifactResponseType = SamlProtocolUtils.buildArtifactResponse(responseDoc);
        Document doc = SamlProtocolUtils.convert(artifactResponseType);
        String artifactResponse = DocumentUtil.asString(doc);

        assertThat(artifactResponse, containsString("samlp:ArtifactResponse"));
        assertThat(artifactResponse, containsString("samlp:Response"));
        assertThat(artifactResponse, containsString("saml:Assertion"));
        assertThat(artifactResponse.indexOf("samlp:ArtifactResponse"), lessThan(artifactResponse.indexOf("samlp:Response")));
        assertThat(artifactResponse.indexOf("samlp:Response"), lessThan(artifactResponse.indexOf("saml:Assertion")));
        assertThat(artifactResponse.split("\\Q<saml:Issuer>http://saml.idp/saml</saml:Issuer>\\E").length, is(4));
        assertThat(artifactResponse.split(
                "\\Q<samlp:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/>\\E").length, is(3));
    }

    @Test
    public void testUnsignedRedirectRejectsRepeatedParameters() {
        for (String name : new String[] {"SAMLRequest", "SAMLResponse", "RelayState", "SigAlg", "Signature"}) {
            String encodedName = "%" + Integer.toHexString(name.charAt(0)) + name.substring(1);
            for (String duplicateName : new String[] {name, encodedName}) {
                MultivaluedMap<String, String> parameters = new MultivaluedHashMap<>();
                UriUtils.parseQueryParameters(name + "=value&" + duplicateName + "=value", true, false).forEach(parameters::put);

                VerificationException exception = assertThrows(VerificationException.class,
                        () -> SamlProtocolUtils.rejectRepeatedRedirectParameters(parameters));
                assertThat(exception.getMessage(), containsString(name));
            }
        }
    }

    @Test
    public void testUnsignedRedirectAcceptsUniqueParameters() throws VerificationException {
        for (String name : new String[] {"SAMLRequest", "SAMLResponse", "%53AMLRequest", "%53AMLResponse"}) {
            MultivaluedMap<String, String> parameters = new MultivaluedHashMap<>();
            UriUtils.parseQueryParameters(name + "=value&%52elayState=relay-state&other=one&other=two", true, false)
                    .forEach(parameters::put);

            SamlProtocolUtils.rejectRepeatedRedirectParameters(parameters);
        }
    }

    @Test
    public void testRedirectSignatureAcceptsSignedMessage() throws Exception {
        String signedQuery = signedRedirectQuery(responseDocument("attacker"), RELAY_STATE);

        verifyRedirect(signedQuery, decodedParam(signedQuery, GeneralConstants.SAML_RESPONSE_KEY), RELAY_STATE);
    }

    @Test
    public void testRedirectSignatureAcceptsSignedMessageWithoutRelayState() throws Exception {
        String signedQuery = signedRedirectQuery(responseDocument("attacker"), null);

        verifyRedirect(signedQuery, decodedParam(signedQuery, GeneralConstants.SAML_RESPONSE_KEY), null);
    }

    @Test
    public void testRedirectSignatureRejectsPercentEncodedMessageName() throws Exception {
        String signedQuery = signedRedirectQuery(responseDocument("attacker"), RELAY_STATE);
        String crafted = RedirectBindingUtil.deflateBase64URLEncode(DocumentUtil.asString(responseDocument("victim")));
        for (String name : new String[] {"%53AMLResponse", "SAML%52esponse"}) {
            String attackQuery = name + "=" + crafted + "&" + signedQuery;

            // @QueryParam resolves the first occurrence of the decoded name, which is the crafted message
            assertRedirectRejected(attackQuery, RedirectBindingUtil.urlDecode(crafted), RELAY_STATE);
            assertRedirectRejected(signedQuery + "&" + name + "=" + crafted,
                    decodedParam(signedQuery, GeneralConstants.SAML_RESPONSE_KEY), RELAY_STATE);
        }
    }

    @Test
    public void testRedirectSignatureRejectsPercentEncodedRelayStateName() throws Exception {
        String signedQuery = signedRedirectQuery(responseDocument("attacker"), RELAY_STATE);
        String attackQuery = "%52elayState=other-session&" + signedQuery;

        assertRedirectRejected(attackQuery, decodedParam(signedQuery, GeneralConstants.SAML_RESPONSE_KEY), "other-session");
    }

    @Test
    public void testRedirectSignatureHandlesEncodedAuthnRequestName() throws Exception {
        Document request = SAML2Request.convert(new SAML2Request().createAuthnRequestType(
                IDGenerator.create("ID_"), "https://sp.example/acs", "https://idp.example/endpoint", "https://sp.example"));

        assertEncodedMessageNames(request, GeneralConstants.SAML_REQUEST_KEY);
    }

    @Test
    public void testRedirectSignatureHandlesEncodedLogoutRequestName() throws Exception {
        Document request = new SAML2LogoutRequestBuilder()
                .destination("https://idp.example/endpoint")
                .issuer("https://sp.example")
                .nameId(SAML2NameIDBuilder.value("attacker").build())
                .sessionIndex("session")
                .buildDocument();

        assertEncodedMessageNames(request, GeneralConstants.SAML_REQUEST_KEY);
    }

    @Test
    public void testRedirectSignatureHandlesEncodedLogoutResponseName() throws Exception {
        Document response = new SAML2LogoutResponseBuilder()
                .destination("https://sp.example/endpoint")
                .issuer("https://idp.example")
                .logoutRequestID(IDGenerator.create("ID_"))
                .buildDocument();

        assertEncodedMessageNames(response, GeneralConstants.SAML_RESPONSE_KEY);
    }

    private static void assertEncodedMessageNames(Document document, String paramKey) throws Exception {
        String signedQuery = signedRedirectQuery(document, RELAY_STATE, paramKey);
        String processed = decodedParam(signedQuery, paramKey);
        verifyRedirect(signedQuery, paramKey, processed, RELAY_STATE);
        String[] names = {paramKey, "%53" + paramKey.substring(1), "SAML%52" + paramKey.substring(5)};
        for (String name : names) {
            verifyRedirect(signedQuery.replace(paramKey + "=", name + "="), paramKey, processed, RELAY_STATE);
        }
        Document crafted = (Document) document.cloneNode(true);
        crafted.getDocumentElement().setAttribute("ID", "unsigned-message");
        String unsigned = RedirectBindingUtil.deflateBase64URLEncode(DocumentUtil.asString(crafted));
        for (String duplicate : names) {
            String parameter = duplicate + "=" + unsigned;
            assertThrows(VerificationException.class,
                    () -> verifyRedirect(parameter + "&" + signedQuery, paramKey, RedirectBindingUtil.urlDecode(unsigned), RELAY_STATE));
            assertThrows(VerificationException.class,
                    () -> verifyRedirect(signedQuery + "&" + parameter, paramKey, processed, RELAY_STATE));
        }
        assertThrows(VerificationException.class,
                () -> verifyRedirect(signedQuery, paramKey, processed, "other-session"));
    }

    @Test
    public void testRedirectSignatureRejectsRepeatedSignatureParameters() throws Exception {
        String signedQuery = signedRedirectQuery(responseDocument("attacker"), RELAY_STATE);
        String processed = decodedParam(signedQuery, GeneralConstants.SAML_RESPONSE_KEY);
        String sigAlg = rawParam(signedQuery, GeneralConstants.SAML_SIG_ALG_REQUEST_KEY);
        String signature = rawParam(signedQuery, GeneralConstants.SAML_SIGNATURE_REQUEST_KEY);

        assertRedirectRejected(signedQuery + "&SigAlg=" + sigAlg, processed, RELAY_STATE);
        assertRedirectRejected(signedQuery + "&Signature=" + signature, processed, RELAY_STATE);
    }

    @Test
    public void testRedirectSignatureRejectsProcessedValuesThatWereNotSigned() throws Exception {
        String signedQuery = signedRedirectQuery(responseDocument("attacker"), RELAY_STATE);
        String processed = decodedParam(signedQuery, GeneralConstants.SAML_RESPONSE_KEY);
        String crafted = RedirectBindingUtil.deflateBase64Encode(DocumentUtil.asString(responseDocument("victim")).getBytes(StandardCharsets.UTF_8));

        assertRedirectRejected(signedQuery, crafted, RELAY_STATE);
        assertRedirectRejected(signedQuery, processed, "other-session");
        assertRedirectRejected(signedQuery, processed, null);

        String queryWithoutRelayState = signedRedirectQuery(responseDocument("attacker"), null);
        assertRedirectRejected(queryWithoutRelayState,
                decodedParam(queryWithoutRelayState, GeneralConstants.SAML_RESPONSE_KEY), RELAY_STATE);
    }

    private static Document responseDocument(String nameId) throws Exception {
        ResponseType response = new SAML2LoginResponseBuilder()
                .requestID(IDGenerator.create("ID_"))
                .destination("https://broker.example/realms/demo/broker/idp/endpoint")
                .issuer("https://idp.example/realms/external")
                .assertionExpiration(1000000)
                .subjectExpiration(1000000)
                .requestIssuer("https://broker.example/realms/demo")
                .nameIdentifier(JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get(), nameId)
                .authMethod(JBossSAMLURIConstants.AC_UNSPECIFIED.get())
                .sessionIndex("idp:" + UUID.randomUUID())
                .buildModel();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new SAMLResponseWriter(StaxUtil.getXMLStreamWriter(bos)).write(response);
        return DocumentUtil.getDocument(new ByteArrayInputStream(bos.toByteArray()));
    }

    private static String signedRedirectQuery(Document document, String relayState) throws Exception {
        return signedRedirectQuery(document, relayState, GeneralConstants.SAML_RESPONSE_KEY);
    }

    private static String signedRedirectQuery(Document document, String relayState, String paramKey) throws Exception {
        BaseSAML2BindingBuilder<?> builder = new BaseSAML2BindingBuilder<>()
                .signWith(null, rsaKeyPair)
                .signatureAlgorithm(SignatureAlgorithm.RSA_SHA256)
                .signDocument();
        if (relayState != null) {
            builder.relayState(relayState);
        }
        return GeneralConstants.SAML_REQUEST_KEY.equals(paramKey)
                ? builder.redirectBinding(document).requestURI("https://broker.example/endpoint").getRawQuery()
                : builder.redirectBinding(document).responseURI("https://broker.example/endpoint").getRawQuery();
    }

    private static String rawParam(String rawQuery, String name) {
        return UriUtils.parseQueryParameters(rawQuery, false).getFirst(name);
    }

    private static String decodedParam(String rawQuery, String name) {
        return UriUtils.parseQueryParameters(rawQuery, true).getFirst(name);
    }

    private static void verifyRedirect(String rawQuery, String processedMessage, String processedRelayState) throws Exception {
        verifyRedirect(rawQuery, GeneralConstants.SAML_RESPONSE_KEY, processedMessage, processedRelayState);
    }

    private static void verifyRedirect(String rawQuery, String paramKey, String processedMessage, String processedRelayState) throws Exception {
        // same parsing as KeycloakUriInfo.getQueryParameters(false)
        MultivaluedMap<String, String> encodedParams = new MultivaluedHashMap<>();
        UriUtils.parseQueryParameters(rawQuery, true, false).forEach(encodedParams::put);

        SAMLDocumentHolder holder = GeneralConstants.SAML_REQUEST_KEY.equals(paramKey)
                ? SAMLRequestParser.parseRequestRedirectBinding(processedMessage)
                : SAMLRequestParser.parseResponseRedirectBinding(processedMessage);
        Assertions.assertNotNull(holder, "The regression must use a valid SAML message");
        SamlProtocolUtils.verifyRedirectSignature(holder, new HardcodedKeyLocator(rsaKeyPair.getPublic()), encodedParams,
                paramKey, processedMessage, processedRelayState);
    }

    private static void assertRedirectRejected(String rawQuery, String processedMessage, String processedRelayState) {
        assertThrows(VerificationException.class, () -> verifyRedirect(rawQuery, processedMessage, processedRelayState));
    }
    @Test
    public void testLimitedInputStream() throws IOException {

        InputStream inputStream = SamlProtocolUtils.limit(new ByteArrayInputStream(bytes), 3);

        for (int i = 1; i <= 3; i++) {
            int read = inputStream.read();
            assertThat(read, is(i));
        }

        Throwable e = Assertions.assertThrowsExactly(IOException.class, () -> {
            inputStream.read();
        });
        assertThat(e.getMessage(), containsString("Limit reached") );

    }

    @Test
    public void testLimitedInputStreamBufferMore() throws IOException {

        InputStream inputStream = SamlProtocolUtils.limit(new ByteArrayInputStream(bytes), 6);

        {
            byte[] buffer = new byte[5];
            int read = inputStream.read(buffer, 0, 2);
            assertThat(read, is(2));
        }
        {
            byte[] buffer = new byte[5];
            int read = inputStream.read(buffer, 0, 2);
            assertThat(read, is(2));
        }

        Throwable e = Assertions.assertThrowsExactly(IOException.class, () -> {
            byte[] buffer = new byte[10];
            int read = inputStream.read(buffer, 0, 10);
        });
        assertThat(e.getMessage(), containsString("Limit reached") );

    }

    @Test
    public void testLimitedInputStreamBufferReadLess() throws IOException {

        InputStream inputStream = SamlProtocolUtils.limit(new ByteArrayInputStream(bytes), 20);

        {
            byte[] buffer = new byte[5];
            int read = inputStream.read(buffer, 0, 5);
            assertThat(read, is(5));
        }
        {
            byte[] buffer = new byte[5];
            int read = inputStream.read(buffer, 0, 3);
            assertThat(read, is(3));
        }
        {
            byte[] buffer = new byte[200];
            int read = inputStream.read(buffer, 0, 20);
            assertThat(read, is(2));
            assertThat(Byte.valueOf(buffer[0]), is(Byte.valueOf((byte)9)));
            assertThat(Byte.valueOf(buffer[1]), is(Byte.valueOf((byte)10)));
        }

    }

    @Test
    public void testLimitedInputStreamBufferRead2() throws IOException {

        InputStream inputStream = SamlProtocolUtils.limit(new ByteArrayInputStream(bytes), 20);
        byte[] buffer = new byte[10];

        int read = inputStream.read(buffer, 0, 5);
        read += inputStream.read(buffer, 5, 3);

        for (int i = 0; i < read; i++) {
            assertThat(bytes[i], is(buffer[i]));
        }

    }


}
