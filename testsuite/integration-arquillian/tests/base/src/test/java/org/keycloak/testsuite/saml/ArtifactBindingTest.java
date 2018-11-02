package org.keycloak.testsuite.saml;

import com.google.common.base.Charsets;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.*;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.profile.util.Soap;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.saml.SAML2LogoutRequestBuilder;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.util.ArtifactResolutionService;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.util.saml.CreateArtifactMessageStepBuilder;
import org.keycloak.testsuite.util.saml.HandleArtifactStepBuilder;
import org.w3c.dom.Document;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import static org.keycloak.testsuite.util.Matchers.isSamlResponse;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;
import static org.keycloak.testsuite.util.SamlClient.Binding.POST;
import static org.keycloak.testsuite.util.SamlClient.Binding.REDIRECT;

public class ArtifactBindingTest extends AbstractSamlTest {

    private final AtomicReference<NameIDType> nameIdRef = new AtomicReference<>();
    private final AtomicReference<String> sessionIndexRef = new AtomicReference<>();

    @Test
    public void testArtifactBindingLoginCheckArtifactWithPost() throws NoSuchAlgorithmException {
        String response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                .transformObject(so -> {
                    so.setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri());
                    return so;
                }).build()
                .login().user(bburkeUser).build().doNotFollowRedirects().executeAndTransform(resp -> EntityUtils.toString(resp.getEntity()));
        assertTrue(response.contains(GeneralConstants.SAML_ARTIFACT_KEY));

        Pattern artifactPattern = Pattern.compile("NAME=\"SAMLart\" VALUE=\"((?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=))");
        Matcher m = artifactPattern.matcher(response);
        assertTrue(m.find());

        String artifactB64 = m.group(1);
        assertNotNull(artifactB64);
        assertFalse(artifactB64.isEmpty());

        byte[] artifact = Base64.getDecoder().decode(artifactB64);
        assertEquals(44, artifact.length);
        assertEquals(0, artifact[0]);
        assertEquals(4, artifact[1]);
        assertEquals(0, artifact[2]);
        assertEquals(0, artifact[3]);

        MessageDigest sha1Digester = MessageDigest.getInstance("SHA-1");
        byte[] source = sha1Digester.digest("http://localhost:8180/auth/realms/demo".getBytes(Charsets.UTF_8));
        for (int i = 0; i < 20; i++) {
            assertEquals(source[i], artifact[i+4]);
        }
    }

    @Test
    public void testArtifactBindingLoginFullExchangeWithPost() {
        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                .transformObject(so -> {
                    so.setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri());
                    return so;
                }).build()
                .login().user(bburkeUser).build().handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST).build()
                .doNotFollowRedirects().executeAndTransform(this::getSoapMessageContents);

        assertTrue(response.getSamlObject() instanceof ArtifactResponseType);
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertEquals(JBossSAMLURIConstants.STATUS_SUCCESS.get(), artifactResponse.getStatus().getStatusCode().getValue().toString());
        assertNull(artifactResponse.getSignature());
        assertTrue(artifactResponse.getAny() instanceof ResponseType);
        ResponseType samlResponse = (ResponseType)artifactResponse.getAny();
        assertEquals(JBossSAMLURIConstants.STATUS_SUCCESS.get(), samlResponse.getStatus().getStatusCode().getValue().toString());
    }


    @Test
    public void testArtifactBindingLoginCorrectSignature() {
        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .transformObject(so -> {
                    so.setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri());
                    return so;
                }).signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY
                        , SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY).build()
                .login().user(bburkeUser).build().handleArtifact(getAuthServerSamlEndpoint(REALM_NAME)
                        , SAML_CLIENT_ID_SALES_POST_SIG).signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY
                        , SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY).build()
                .doNotFollowRedirects().executeAndTransform(this::getSoapMessageContents);

        assertTrue(response.getSamlObject() instanceof ArtifactResponseType);
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertEquals(JBossSAMLURIConstants.STATUS_SUCCESS.get(), artifactResponse.getStatus().getStatusCode().getValue().toString());
        assertTrue(artifactResponse.getAny() instanceof ResponseType);
        ResponseType samlResponse = (ResponseType)artifactResponse.getAny();
        assertEquals(JBossSAMLURIConstants.STATUS_SUCCESS.get(), samlResponse.getStatus().getStatusCode().getValue().toString());
    }

    @Test
    public void testArtifactBindingLoginIncorrectSignature() {
        Document response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .transformObject(so -> {
                    so.setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri());
                    return so;
                }).signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY
                , SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY).build()
                .login().user(bburkeUser).build().handleArtifact(getAuthServerSamlEndpoint(REALM_NAME)
                        , SAML_CLIENT_ID_SALES_POST_SIG).signWith(SAML_CLIENT_SALES_POST_SIG_EXPIRED_PRIVATE_KEY,
                        SAML_CLIENT_SALES_POST_SIG_EXPIRED_PUBLIC_KEY).build()
                .doNotFollowRedirects().executeAndTransform(this::extractSoapMessage);

        String soapMessage = DocumentUtil.asString(response);
        assertFalse(soapMessage.contains("ArtifactResponse"));
        assertTrue(soapMessage.contains("invalid_signature"));
    }

    @Test
    public void testArtifactBindingLoginGetArtifactResponseTwice() {
        SamlClientBuilder clientBuilder = new SamlClientBuilder();
        HandleArtifactStepBuilder handleArifactBuilder = new HandleArtifactStepBuilder(
                getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, clientBuilder);

        Document response= clientBuilder.authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.REDIRECT)
                .transformObject(so -> {
                    so.setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri());
                    return so;
                }).build()
                .login().user(bburkeUser).build().handleArtifact(handleArifactBuilder).build()
                .handleArtifact(handleArifactBuilder).replayPost(true).build().doNotFollowRedirects().executeAndTransform(this::extractSoapMessage);

        String soapMessage = DocumentUtil.asString(response);
        assertFalse(soapMessage.contains("ArtifactResponse"));
        assertTrue(soapMessage.contains("Cannot find artifact"));
    }

    @Test
    public void testArtifactBindingLoginForceArifactBinding() {
        ClientRepresentation sales2Rep = adminClient.realm(REALM_NAME).clients().findByClientId(SAML_CLIENT_ID_SALES_POST).get(0);

        adminClient.realm(REALM_NAME)
                .clients().get(sales2Rep.getId())
                .update(ClientBuilder.edit(sales2Rep)
                        .attribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true")
                        .build());

        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                .build()
                .login().user(bburkeUser).build().handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST).build()
                .doNotFollowRedirects().executeAndTransform(this::getSoapMessageContents);

        assertTrue(response.getSamlObject() instanceof ArtifactResponseType);
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertEquals(JBossSAMLURIConstants.STATUS_SUCCESS.get(), artifactResponse.getStatus().getStatusCode().getValue().toString());
        assertTrue(artifactResponse.getAny() instanceof ResponseType);
        ResponseType samlResponse = (ResponseType)artifactResponse.getAny();
        assertEquals(JBossSAMLURIConstants.STATUS_SUCCESS.get(), samlResponse.getStatus().getStatusCode().getValue().toString());
    }

    @Test
    public void testArtifactBindingLoginSignedArtifactResponse() {
        ClientRepresentation salesRep = adminClient.realm(REALM_NAME).clients().findByClientId(SAML_CLIENT_ID_SALES_POST).get(0);

        adminClient.realm(REALM_NAME)
                .clients().get(salesRep.getId())
                .update(ClientBuilder.edit(salesRep)
                        .attribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true")
                        .attribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true")
                        .build());

        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                .build()
                .login().user(bburkeUser).build().handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST).build()
                .doNotFollowRedirects().executeAndTransform(this::getSoapMessageContents);


        assertTrue(response.getSamlObject() instanceof ArtifactResponseType);
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertEquals(JBossSAMLURIConstants.STATUS_SUCCESS.get(), artifactResponse.getStatus().getStatusCode().getValue().toString());
        assertNotNull(artifactResponse.getSignature());
        assertTrue(artifactResponse.getAny() instanceof ResponseType);
        ResponseType samlResponse = (ResponseType)artifactResponse.getAny();
        assertEquals(JBossSAMLURIConstants.STATUS_SUCCESS.get(), samlResponse.getStatus().getStatusCode().getValue().toString());
    }

    @Test
    public void testArtifactBindingLoginFullExchangeWithRedirect() {
        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.REDIRECT)
                .transformObject(so -> {
                    so.setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri());
                    return so;
                }).build()
                .login().user(bburkeUser).build().handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST).verifyRedirect(true).build()
                .doNotFollowRedirects().executeAndTransform(this::getSoapMessageContents);

        assertTrue(response.getSamlObject() instanceof ArtifactResponseType);
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertEquals(JBossSAMLURIConstants.STATUS_SUCCESS.get(), artifactResponse.getStatus().getStatusCode().getValue().toString());
        assertTrue(artifactResponse.getAny() instanceof ResponseType);
        ResponseType samlResponse = (ResponseType)artifactResponse.getAny();
        assertEquals(JBossSAMLURIConstants.STATUS_SUCCESS.get(), samlResponse.getStatus().getStatusCode().getValue().toString());
    }


    @Test
    public void testReceiveArtifactLoginFullWithPost() throws ParsingException, ConfigurationException, ProcessingException, InterruptedException {

        ClientRepresentation salesRep = adminClient.realm(REALM_NAME).clients().findByClientId(SAML_CLIENT_ID_SALES_POST).get(0);
        adminClient.realm(REALM_NAME)
                .clients().get(salesRep.getId())
                .update(ClientBuilder.edit(salesRep)
                        .attribute(SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE, "http://127.0.0.1:8082/")
                        .build());

        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, AbstractSamlTest.SAML_ASSERTION_CONSUMER_URL_SALES_POST, null);
        Document doc = SAML2Request.convert(loginRep);

        SamlClientBuilder builder = new SamlClientBuilder();
        CreateArtifactMessageStepBuilder camb = new CreateArtifactMessageStepBuilder(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SamlClient.Binding.POST, builder);

        ArtifactResolutionService ars = new ArtifactResolutionService("http://127.0.0.1:8082/").setResponseDocument(doc);
        try {
            Thread arsThread = new Thread(ars);
            arsThread.start();
            synchronized (ars) {
                ars.wait();
                SAMLDocumentHolder response = builder.artifactMessage(camb).build().login().user(bburkeUser).build().getSamlResponse(SamlClient.Binding.POST);
                assertTrue(response.getSamlObject() instanceof ResponseType);
                ResponseType rt = (ResponseType)response.getSamlObject();
                assertFalse(rt.getAssertions().isEmpty());
                assertNotNull(ars.getLastArtifactResolve());
                assertEquals(camb.getLastArtifact(), ars.getLastArtifactResolve().getArtifact());
            }
        } finally {
            ars.stop();
        }
    }

    @Test
    public void testReceiveArtifactLoginFullWithRedirect() throws ParsingException, ConfigurationException, ProcessingException, InterruptedException {
        ClientRepresentation salesRep = adminClient.realm(REALM_NAME).clients().findByClientId(SAML_CLIENT_ID_SALES_POST).get(0);
        adminClient.realm(REALM_NAME)
                .clients().get(salesRep.getId())
                .update(ClientBuilder.edit(salesRep)
                        .attribute(SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE, "http://127.0.0.1:8082/")
                        .build());

        AuthnRequestType loginReq = SamlClient.createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, AbstractSamlTest.SAML_ASSERTION_CONSUMER_URL_SALES_POST, null);
        loginReq.setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri());
        Document doc = SAML2Request.convert(loginReq);

        SamlClientBuilder builder = new SamlClientBuilder();
        CreateArtifactMessageStepBuilder camb = new CreateArtifactMessageStepBuilder(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SamlClient.Binding.REDIRECT, builder);

        ArtifactResolutionService ars = new ArtifactResolutionService("http://127.0.0.1:8082/").setResponseDocument(doc);
        try {
            Thread arsThread = new Thread(ars);
            arsThread.start();
            synchronized (ars) {
                ars.wait();
                SAMLDocumentHolder response = builder.artifactMessage(camb).build().login().user(bburkeUser).build().getSamlResponse(SamlClient.Binding.REDIRECT);
                assertTrue(response.getSamlObject() instanceof ResponseType);
                ResponseType rt = (ResponseType)response.getSamlObject();
                assertFalse(rt.getAssertions().isEmpty());
                assertNotNull(ars.getLastArtifactResolve());
                assertEquals(camb.getLastArtifact(), ars.getLastArtifactResolve().getArtifact());
            }
        } finally {
            ars.stop();
        }
    }

    @Test
    public void testReceiveArtifactNonExistingClient() throws ParsingException, ConfigurationException, ProcessingException, InterruptedException {
        ClientRepresentation salesRep = adminClient.realm(REALM_NAME).clients().findByClientId(SAML_CLIENT_ID_SALES_POST).get(0);
        adminClient.realm(REALM_NAME)
                .clients().get(salesRep.getId())
                .update(ClientBuilder.edit(salesRep)
                        .attribute(SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE, "http://127.0.0.1:8082/")
                        .build());

        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument("blabla", AbstractSamlTest.SAML_ASSERTION_CONSUMER_URL_SALES_POST, null);
        Document doc = SAML2Request.convert(loginRep);

        SamlClientBuilder builder = new SamlClientBuilder();
        CreateArtifactMessageStepBuilder camb = new CreateArtifactMessageStepBuilder(getAuthServerSamlEndpoint(REALM_NAME), "blabla",
                SamlClient.Binding.POST, builder);

        ArtifactResolutionService ars = new ArtifactResolutionService("http://127.0.0.1:8082/").setResponseDocument(doc);
        try {
            Thread arsThread = new Thread(ars);
            arsThread.start();
            synchronized (ars) {
                ars.wait();
                String response = builder.artifactMessage(camb).build().executeAndTransform(resp -> EntityUtils.toString(resp.getEntity()));
                assertTrue(response.contains("Invalid Request"));
            }
        } finally {
            ars.stop();
        }
    }

    @Test
    public void testReceiveArtifactLogoutFullWithPost() throws ParsingException, ConfigurationException, ProcessingException, InterruptedException {
        ClientRepresentation salesRep = adminClient.realm(REALM_NAME).clients().findByClientId(SAML_CLIENT_ID_SALES_POST).get(0);
        adminClient.realm(REALM_NAME)
                .clients().get(salesRep.getId())
                .update(ClientBuilder.edit(salesRep)
                        .attribute(SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE, "http://127.0.0.1:8082/")
                        .build());

        SamlClientBuilder builder = new SamlClientBuilder();
        CreateArtifactMessageStepBuilder camb = new CreateArtifactMessageStepBuilder(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SamlClient.Binding.POST, builder);

        ArtifactResolutionService ars = new ArtifactResolutionService("http://127.0.0.1:8082/");
        try {
            Thread arsThread = new Thread(ars);
            arsThread.start();
            synchronized (ars) {
                ars.wait();
                SAMLDocumentHolder samlResponse = builder.authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST).build()
                        .login().user(bburkeUser).build()
                        .processSamlResponse(POST)
                        .transformObject(x -> {
                            SAML2Object samlObj =  extractNameIdAndSessionIndexAndTerminate(x);
                            setArtifactResolutionServiceLogoutRequest(ars);
                            return samlObj;
                        })
                        .build().artifactMessage(camb).build().getSamlResponse(POST);
               assertTrue(samlResponse.getSamlObject() instanceof StatusResponseType);
               StatusResponseType srt = (StatusResponseType) samlResponse.getSamlObject();
               assertEquals(JBossSAMLURIConstants.STATUS_SUCCESS.get(), srt.getStatus().getStatusCode().getValue().toString());
               assertEquals(camb.getLastArtifact(), ars.getLastArtifactResolve().getArtifact());
            }
        } finally {
            ars.stop();
        }
    }

    @Test
    public void testReceiveArtifactLogoutFullWithRedirect() throws ParsingException, ConfigurationException, ProcessingException, InterruptedException {
        ClientRepresentation salesRep = adminClient.realm(REALM_NAME).clients().findByClientId(SAML_CLIENT_ID_SALES_POST).get(0);
        adminClient.realm(REALM_NAME)
                .clients().get(salesRep.getId())
                .update(ClientBuilder.edit(salesRep)
                        .frontchannelLogout(true)
                        .attribute(SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE, "http://127.0.0.1:8082/")
                        .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, "http://url")
                        .build());

        SamlClientBuilder builder = new SamlClientBuilder();
        CreateArtifactMessageStepBuilder camb = new CreateArtifactMessageStepBuilder(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                REDIRECT, builder);

        ArtifactResolutionService ars = new ArtifactResolutionService("http://127.0.0.1:8082/");
        try {
            Thread arsThread = new Thread(ars);
            arsThread.start();
            synchronized (ars) {
                ars.wait();
                SAMLDocumentHolder samlResponse = builder
                        .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                                SAML_ASSERTION_CONSUMER_URL_SALES_POST, REDIRECT)
                        .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri()).build()
                        .login().user(bburkeUser).build()
                        .processSamlResponse(REDIRECT)
                        .transformObject(x -> {
                            SAML2Object samlObj =  extractNameIdAndSessionIndexAndTerminate(x);
                            setArtifactResolutionServiceLogoutRequest(ars);
                            return samlObj;
                        })
                        .build().artifactMessage(camb).build().getSamlResponse(REDIRECT);
                assertTrue(samlResponse.getSamlObject() instanceof StatusResponseType);
                StatusResponseType srt = (StatusResponseType) samlResponse.getSamlObject();
                assertEquals(JBossSAMLURIConstants.STATUS_SUCCESS.get(), srt.getStatus().getStatusCode().getValue().toString());
                assertEquals(camb.getLastArtifact(), ars.getLastArtifactResolve().getArtifact());
            }
        } finally {
            ars.stop();
        }
    }

    private SAMLDocumentHolder getSoapMessageContents(CloseableHttpResponse response) throws IOException, ParsingException, ProcessingException {
        assertThat(response, statusCodeIsHC(Response.Status.OK));
        Document soapBody = extractSoapMessage(response);
        return SAML2Request.getSAML2ObjectFromDocument(soapBody);
    }

    private Document extractSoapMessage(CloseableHttpResponse response) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(EntityUtils.toByteArray(response.getEntity()));
        Document soapBody = Soap.extractSoapMessage(bais);
        response.close();
        return soapBody;
    }

    //TODO methods that are here AND in logoutTest should be made public or set elsewhere?
    private SAML2Object extractNameIdAndSessionIndexAndTerminate(SAML2Object so) {
        assertThat(so, isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        ResponseType loginResp1 = (ResponseType) so;
        final AssertionType firstAssertion = loginResp1.getAssertions().get(0).getAssertion();
        assertThat(firstAssertion, org.hamcrest.Matchers.notNullValue());
        assertThat(firstAssertion.getSubject().getSubType().getBaseID(), instanceOf(NameIDType.class));

        NameIDType nameId = (NameIDType) firstAssertion.getSubject().getSubType().getBaseID();
        AuthnStatementType firstAssertionStatement = (AuthnStatementType) firstAssertion.getStatements().iterator().next();

        nameIdRef.set(nameId);
        sessionIndexRef.set(firstAssertionStatement.getSessionIndex());

        return null;
    }

    private void setArtifactResolutionServiceLogoutRequest(ArtifactResolutionService ars) throws ParsingException, ConfigurationException, ProcessingException {
        SAML2LogoutRequestBuilder builder = new SAML2LogoutRequestBuilder()
                .destination(getAuthServerSamlEndpoint(REALM_NAME).toString())
                .issuer(SAML_CLIENT_ID_SALES_POST)
                .sessionIndex(sessionIndexRef.get());

        final NameIDType nameIdValue = nameIdRef.get();

        if (nameIdValue != null) {
            builder = builder.userPrincipal(nameIdValue.getValue(), nameIdValue.getFormat() == null ? null : nameIdValue.getFormat().toString());
        }
        ars.setResponseDocument(builder.buildDocument());
    }

}
