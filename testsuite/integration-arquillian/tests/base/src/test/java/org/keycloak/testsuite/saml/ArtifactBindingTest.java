package org.keycloak.testsuite.saml;

import com.google.common.base.Charsets;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.dom.saml.v2.protocol.ArtifactResponseType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.NameIDMappingResponseType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlProtocolUtils;
import org.keycloak.protocol.saml.profile.util.Soap;
import org.keycloak.protocol.saml.util.ArtifactBindingUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.saml.SAML2LogoutResponseBuilder;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.sessions.CommonClientSessionModel;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.InfinispanTestTimeServiceRule;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.util.SamlUtils;
import org.keycloak.testsuite.util.saml.HandleArtifactStepBuilder;
import org.keycloak.testsuite.util.saml.SamlMessageReceiver;
import org.keycloak.testsuite.util.saml.SessionStateChecker;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.w3c.dom.Document;

import javax.ws.rs.core.Response;
import javax.xml.transform.dom.DOMSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.keycloak.testsuite.util.Matchers.bodyHC;
import static org.keycloak.testsuite.util.Matchers.isSamlLogoutRequest;
import static org.keycloak.testsuite.util.Matchers.isSamlResponse;
import static org.keycloak.testsuite.util.Matchers.isSamlStatusResponse;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;
import static org.keycloak.testsuite.util.SamlClient.Binding.ARTIFACT_RESPONSE;
import static org.keycloak.testsuite.util.SamlClient.Binding.POST;
import static org.keycloak.testsuite.util.SamlClient.Binding.REDIRECT;
import static org.keycloak.testsuite.util.SamlUtils.getSPInstallationDescriptor;

public class ArtifactBindingTest extends AbstractSamlTest {

    @Rule
    public InfinispanTestTimeServiceRule ispnTestTimeService = new InfinispanTestTimeServiceRule(this);

    /************************ LOGIN TESTS ************************/

    @Test
    public void testArtifactBindingTimesOutAfterCodeToTokenLifespan() throws Exception {
        
        getCleanup().addCleanup(
                new RealmAttributeUpdater(adminClient.realm(REALM_NAME))
                        .setAccessCodeLifespan(1)
                        .update()
        );

        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri())
                .build()
                .login().user(bburkeUser).build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST)
                    .setBeforeStepChecks(() -> setTimeOffset(1000)) // Move in time before resolving the artifact
                .build()
                .doNotFollowRedirects()
                .executeAndTransform(this::getArtifactResponse);

        assertThat(response.getSamlObject(), instanceOf(ArtifactResponseType.class));
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertThat(artifactResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(artifactResponse.getAny(), nullValue());
    }

    @Test
    public void testArtifactBindingWithResponseAndAssertionSignature() throws Exception {
        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_ASSERTION_AND_RESPONSE_SIG,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST_ASSERTION_AND_RESPONSE_SIG, POST)
                    .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri())
                    .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                    .build()
                .login()
                    .user(bburkeUser)
                    .build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_ASSERTION_AND_RESPONSE_SIG)
                    .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                    .build()
                .doNotFollowRedirects()
                .executeAndTransform(this::getArtifactResponse);

        assertThat(response.getSamlObject(), instanceOf(ArtifactResponseType.class));
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertThat(artifactResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(artifactResponse.getAny(), instanceOf(ResponseType.class));
        ResponseType samlResponse = (ResponseType)artifactResponse.getAny();
        assertThat(samlResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(samlResponse.getAssertions().get(0).getAssertion().getSignature(), not(nullValue()));

        SamlDeployment deployment = SamlUtils.getSamlDeploymentForClient("sales-post-assertion-and-response-sig");
        SamlProtocolUtils.verifyDocumentSignature(response.getSamlDocument(), deployment.getIDP().getSignatureValidationKeyLocator()); // Checks the signature of the response as well as the signature of the assertion
    }

    @Test
    public void testArtifactBindingWithEncryptedAssertion() throws Exception {
        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_ENC,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST_ENC, POST)
                .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri())
                .signWith(SAML_CLIENT_SALES_POST_ENC_PRIVATE_KEY, SAML_CLIENT_SALES_POST_ENC_PUBLIC_KEY)
                .build()
                .login().user(bburkeUser).build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_ENC)
                .signWith(SAML_CLIENT_SALES_POST_ENC_PRIVATE_KEY, SAML_CLIENT_SALES_POST_ENC_PUBLIC_KEY)
                .build()
                .doNotFollowRedirects()
                .executeAndTransform(ARTIFACT_RESPONSE::extractResponse);

        assertThat(response.getSamlObject(), instanceOf(ResponseType.class));
        ResponseType loginResponse = (ResponseType) response.getSamlObject();
        assertThat(loginResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(loginResponse.getAssertions().get(0).getAssertion(), nullValue());
        assertThat(loginResponse.getAssertions().get(0).getEncryptedAssertion(), not(nullValue()));

        SamlDeployment deployment = SamlUtils.getSamlDeploymentForClient("sales-post-enc");
        AssertionUtil.decryptAssertion(response, loginResponse, deployment.getDecryptionKey());

        assertThat(loginResponse.getAssertions().get(0).getAssertion(), not(nullValue()));
        assertThat(loginResponse.getAssertions().get(0).getEncryptedAssertion(), nullValue());
        assertThat(loginResponse.getAssertions().get(0).getAssertion().getIssuer().getValue(), equalTo(getAuthServerRealmBase(REALM_NAME).toString()));
    }

    @Test
    public void testArtifactBindingLoginCheckArtifactWithPost() throws NoSuchAlgorithmException {
        String response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                    .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri())
                .build()
                .login().user(bburkeUser).build().doNotFollowRedirects().executeAndTransform(resp -> EntityUtils.toString(resp.getEntity()));
        assertThat(response, containsString(GeneralConstants.SAML_ARTIFACT_KEY));

        Pattern artifactPattern = Pattern.compile("NAME=\"SAMLart\" VALUE=\"((?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=))");
        Matcher m = artifactPattern.matcher(response);
        assertThat(m.find(), is(true));

        String artifactB64 = m.group(1);
        assertThat(artifactB64,not(isEmptyOrNullString()));

        byte[] artifact = Base64.getDecoder().decode(artifactB64);
        assertThat(artifact.length, is(44));
        assertThat(artifact[0], is((byte)0));
        assertThat(artifact[1], is((byte)4));
        assertThat(artifact[2], is((byte)0));
        assertThat(artifact[3], is((byte)0));

        MessageDigest sha1Digester = MessageDigest.getInstance("SHA-1");
        byte[] source = sha1Digester.digest(getAuthServerRealmBase(REALM_NAME).toString().getBytes(Charsets.UTF_8));
        for (int i = 0; i < 20; i++) {
            assertThat(source[i], is(artifact[i+4]));
        }
    }

    @Test
    public void testArtifactBindingLoginFullExchangeWithPost() {
        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                    .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri())
                .build()
                .login().user(bburkeUser).build().handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST).build()
                .doNotFollowRedirects().executeAndTransform(this::getArtifactResponse);

        assertThat(response.getSamlObject(), instanceOf(ArtifactResponseType.class));
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertThat(artifactResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(artifactResponse.getSignature(), nullValue());
        assertThat(artifactResponse.getAny(), instanceOf(ResponseType.class));
        assertThat(artifactResponse.getInResponseTo(), not(isEmptyOrNullString()));
        ResponseType samlResponse = (ResponseType)artifactResponse.getAny();
        assertThat(samlResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
    }


    @Test
    public void testArtifactBindingLoginCorrectSignature() {
        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                    .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri())
                    .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY
                        , SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(bburkeUser).build().handleArtifact(getAuthServerSamlEndpoint(REALM_NAME)
                        , SAML_CLIENT_ID_SALES_POST_SIG).signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY
                        , SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY).build()
                .doNotFollowRedirects().executeAndTransform(this::getArtifactResponse);

        assertThat(response.getSamlObject(), instanceOf(ArtifactResponseType.class));
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertThat(artifactResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(artifactResponse.getAny(), instanceOf(ResponseType.class));
        assertThat(artifactResponse.getSignature(), not(nullValue()));

        ResponseType samlResponse = (ResponseType)artifactResponse.getAny();
        assertThat(samlResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(samlResponse.getAssertions().get(0).getAssertion().getSignature(), nullValue());

    }

    @Test
    public void testArtifactBindingLoginIncorrectSignature() {
        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                    .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri())
                    .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY
                    , SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(bburkeUser).build()

                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME)
                        , SAML_CLIENT_ID_SALES_POST_SIG).signWith(SAML_CLIENT_SALES_POST_SIG_EXPIRED_PRIVATE_KEY,
                        SAML_CLIENT_SALES_POST_SIG_EXPIRED_PUBLIC_KEY)
                .build()
                .doNotFollowRedirects()
                .executeAndTransform(this::getArtifactResponse);

        assertThat(response.getSamlObject(), instanceOf(ArtifactResponseType.class));
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertThat(artifactResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(artifactResponse.getAny(), nullValue());
        assertThat(artifactResponse.getSignature(), not(nullValue()));
    }

    @Test
    public void testArtifactBindingLoginGetArtifactResponseTwice() {
        SamlClientBuilder clientBuilder = new SamlClientBuilder();
        HandleArtifactStepBuilder handleArtifactBuilder = new HandleArtifactStepBuilder(
                getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, clientBuilder);

        SAMLDocumentHolder response= clientBuilder.authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.REDIRECT)
                    .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri())
                .build()
                .login().user(bburkeUser).build()
                .handleArtifact(handleArtifactBuilder).build()
                .processSamlResponse(ARTIFACT_RESPONSE)
                    .transformObject(ob -> {
                        assertThat(ob, isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
                        return null;
                    })
                .build()
                .handleArtifact(handleArtifactBuilder).replayPost(true).build()
                .doNotFollowRedirects()
                .executeAndTransform(this::getArtifactResponse);

        assertThat(response.getSamlObject(), instanceOf(ArtifactResponseType.class));
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertThat(artifactResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(artifactResponse.getAny(), nullValue());
    }

    @Test
    public void testArtifactSuccessfulAfterFirstUnsuccessfulRequest() {
        SamlClientBuilder clientBuilder = new SamlClientBuilder();

        AtomicReference<String> artifact = new AtomicReference<>();

        SAMLDocumentHolder response = clientBuilder.authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri())
                .build()
                .login().user(bburkeUser).build()

                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST2) // Wrong issuer
                .storeArtifact(artifact)
                .build()
                .assertResponse(r -> assertThat(r, bodyHC(containsString(JBossSAMLURIConstants.STATUS_REQUEST_DENIED.get()))))

                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST)
                .useArtifact(artifact)
                .build()
                .executeAndTransform(ARTIFACT_RESPONSE::extractResponse);

        assertThat(response.getSamlObject(), isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
    }

    @Test
    public void testArtifactBindingLoginForceArtifactBinding() {
        getCleanup()
            .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                    .setAttribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true")
                    .update()
            );

        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                .build()
                .login().user(bburkeUser).build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST).build()
                .doNotFollowRedirects()
                .executeAndTransform(this::getArtifactResponse);

        assertThat(response.getSamlObject(), instanceOf(ArtifactResponseType.class));
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertThat(artifactResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(artifactResponse.getAny(), instanceOf(ResponseType.class));
        ResponseType samlResponse = (ResponseType)artifactResponse.getAny();
        assertThat(samlResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
    }

    @Test
    public void testArtifactBindingLoginSignedArtifactResponse() throws Exception {
        getCleanup()
            .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                    .setAttribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true")
                    .setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true")
                    .update()
            );

        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                .build()
                .login().user(bburkeUser).build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST).build()
                .doNotFollowRedirects()
                .executeAndTransform(this::getArtifactResponse);


        assertThat(response.getSamlObject(), instanceOf(ArtifactResponseType.class));
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertThat(artifactResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(artifactResponse.getSignature(), notNullValue());
        assertThat(artifactResponse.getAny(), instanceOf(ResponseType.class));
        assertThat(artifactResponse.getInResponseTo(), not(isEmptyOrNullString()));
        ResponseType samlResponse = (ResponseType)artifactResponse.getAny();
        assertThat(samlResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));

        SamlDeployment deployment = SamlUtils.getSamlDeploymentForClient("sales-post");
        SamlProtocolUtils.verifyDocumentSignature(response.getSamlDocument(), deployment.getIDP().getSignatureValidationKeyLocator());
    }

    @Test
    public void testArtifactBindingLoginFullExchangeWithRedirect() {
        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.REDIRECT)
                    .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri())
                .build()
                .login().user(bburkeUser).build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST).verifyRedirect(true).build()
                .doNotFollowRedirects()
                .executeAndTransform(this::getArtifactResponse);

        assertThat(response.getSamlObject(), instanceOf(ArtifactResponseType.class));
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertThat(artifactResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(artifactResponse.getAny(), instanceOf(ResponseType.class));
        assertThat(artifactResponse.getInResponseTo(), not(isEmptyOrNullString()));
        ResponseType samlResponse = (ResponseType)artifactResponse.getAny();
        assertThat(samlResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
    }

    @Test
    public void testArtifactResponseContainsCorrectInResponseTo(){
        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                    .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri())
                .build()
                .login().user(bburkeUser).build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST).setArtifactResolveId("TestId").build()
                .doNotFollowRedirects()
                .executeAndTransform(this::getArtifactResponse);

        assertThat(response.getSamlObject(), instanceOf(ArtifactResponseType.class));
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertThat(artifactResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(artifactResponse.getSignature(), nullValue());
        assertThat(artifactResponse.getAny(), instanceOf(ResponseType.class));
        assertThat(artifactResponse.getInResponseTo(), is("TestId"));
    }

    /************************ LOGOUT TESTS ************************/

    @Test
    public void testArtifactBindingLogoutSingleClientCheckArtifact() throws NoSuchAlgorithmException {
        getCleanup()
            .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                    .setAttribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true")
                    .setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true")
                    .setAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE, "http://url")
                    .setFrontchannelLogout(true)
                    .update()
            );

        String response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                .build()
                .login().user(bburkeUser).build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST).build()
                .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SamlClient.Binding.POST).build()
                .doNotFollowRedirects()
                .executeAndTransform(resp -> EntityUtils.toString(resp.getEntity()));

        assertThat(response, containsString(GeneralConstants.SAML_ARTIFACT_KEY));
        Pattern artifactPattern = Pattern.compile("NAME=\"SAMLart\" VALUE=\"((?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=))");
        Matcher m = artifactPattern.matcher(response);
        assertThat(true, is(m.find()));

        String artifactB64 = m.group(1);
        assertThat(artifactB64, not(isEmptyOrNullString()));

        byte[] artifact = Base64.getDecoder().decode(artifactB64);
        assertThat(artifact.length, is(44));
        assertThat(artifact[0], is((byte)0));
        assertThat(artifact[1], is((byte)4));
        assertThat(artifact[2], is((byte)0));
        assertThat(artifact[3], is((byte)0));

        MessageDigest sha1Digester = MessageDigest.getInstance("SHA-1");
        byte[] source = sha1Digester.digest(getAuthServerRealmBase(REALM_NAME).toString().getBytes(Charsets.UTF_8));
        for (int i = 0; i < 20; i++) {
            assertThat(source[i], is(artifact[i+4]));
        }
    }

    @Test
    public void testArtifactBindingLogoutSingleClientPost() {
        getCleanup()
            .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                    .setAttribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true")
                    .setAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE, "http://url")
                    .setFrontchannelLogout(true)
                    .update()
            );

        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST)
                .build()
                .login().user(bburkeUser).build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST).build()
                .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, POST).build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST).build()
                .doNotFollowRedirects()
                .executeAndTransform(this::getArtifactResponse);

        assertThat(response.getSamlObject(), instanceOf(ArtifactResponseType.class));
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertThat(artifactResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(artifactResponse.getSignature(), nullValue());
        assertThat(artifactResponse.getAny(), not(instanceOf(ResponseType.class)));
        assertThat(artifactResponse.getAny(), not(instanceOf(ArtifactResponseType.class)));
        assertThat(artifactResponse.getAny(), not(instanceOf(NameIDMappingResponseType.class)));
        assertThat(artifactResponse.getAny(), instanceOf(StatusResponseType.class));
        StatusResponseType samlResponse = (StatusResponseType)artifactResponse.getAny();
        assertThat(samlResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
    }

    @Test
    public void testArtifactBindingLogoutSingleClientRedirect() {
        getCleanup()
            .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                    .setAttribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true")
                    .setAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE, "http://url")
                    .setFrontchannelLogout(true)
                    .update()
            );

        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, REDIRECT)
                .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri())
                .build()
                .login().user(bburkeUser).build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST).verifyRedirect(true).build()
                .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, REDIRECT).build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST).verifyRedirect(true).build()
                .doNotFollowRedirects()
                .executeAndTransform(this::getArtifactResponse);

        assertThat(response.getSamlObject(), instanceOf(ArtifactResponseType.class));
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertThat(artifactResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(artifactResponse.getSignature(), nullValue());
        assertThat(artifactResponse.getAny(), not(instanceOf(ResponseType.class)));
        assertThat(artifactResponse.getAny(), not(instanceOf(ArtifactResponseType.class)));
        assertThat(artifactResponse.getAny(), not(instanceOf(NameIDMappingResponseType.class)));
        assertThat(artifactResponse.getAny(), instanceOf(StatusResponseType.class));
        StatusResponseType samlResponse = (StatusResponseType)artifactResponse.getAny();
        assertThat(samlResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
    }

    @Test
    public void testArtifactBindingLogoutTwoClientsPostWithSig() throws Exception {
        getCleanup()
            .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST_SIG)
                    .setAttribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true")
                    .setAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE, "http://url")
                    .setFrontchannelLogout(true)
                    .update()
            );

        SAMLDocumentHolder response = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST2, SAML_ASSERTION_CONSUMER_URL_SALES_POST2, POST).build()
                .login().user(bburkeUser).build()
                .processSamlResponse(POST)
                    .transformObject(this::extractNameIdAndSessionIndexAndTerminate)
                .build()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG, SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, POST)
                    .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().sso(true).build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG)
                    .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST2, POST)
                    .nameId(nameIdRef::get)
                    .sessionIndex(sessionIndexRef::get)
                .build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG)
                    .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .doNotFollowRedirects()
                .executeAndTransform(this::getArtifactResponse);

        assertThat(response.getSamlObject(), instanceOf(ArtifactResponseType.class));
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertThat(artifactResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(artifactResponse.getSignature(), notNullValue());
        assertThat(artifactResponse.getAny(), instanceOf(LogoutRequestType.class));

        SamlDeployment deployment = SamlUtils.getSamlDeploymentForClient("sales-post");
        SamlProtocolUtils.verifyDocumentSignature(response.getSamlDocument(), deployment.getIDP().getSignatureValidationKeyLocator());
    }

    @Test
    public void testArtifactBindingLogoutTwoClientsRedirect() {
        getCleanup()
            .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                    .setAttribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true")
                    .setAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE, "http://url")
                    .setFrontchannelLogout(true)
                    .update()
            )
            .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST2)
                    .setAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE, "http://url")
                    .setFrontchannelLogout(true)
                    .update()
            );

        SAMLDocumentHolder response = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST2, SAML_ASSERTION_CONSUMER_URL_SALES_POST2, REDIRECT)
                    .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri()).build()
                .login().user(bburkeUser).build()
                .processSamlResponse(REDIRECT)
                    .transformObject(this::extractNameIdAndSessionIndexAndTerminate)
                .build()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST, REDIRECT).setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri())
                .build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST).verifyRedirect(true).build()   // This is a formal step
                .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST2, REDIRECT)
                    .nameId(nameIdRef::get)
                    .sessionIndex(sessionIndexRef::get)
                .build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST).verifyRedirect(true).build()
                .doNotFollowRedirects()
                .executeAndTransform(this::getArtifactResponse);

        assertThat(response.getSamlObject(), instanceOf(ArtifactResponseType.class));
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertThat(artifactResponse.getSignature(), nullValue());
        assertThat(artifactResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(artifactResponse.getAny(), instanceOf(LogoutRequestType.class));
    }

    @Test
    public void testArtifactBindingWithBackchannelLogout() {
        try (SamlMessageReceiver backchannelLogoutReceiver = new SamlMessageReceiver(8082);
             ClientAttributeUpdater cau = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                     .setAttribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true")
                     .setFrontchannelLogout(false)
                     .setAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, backchannelLogoutReceiver.getUrl())
                     .update()) {

            new SamlClientBuilder()
                    .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST)
                    .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri()).build()
                    .login().user(bburkeUser).build()
                    .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST).build()

                    .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST2, SAML_ASSERTION_CONSUMER_URL_SALES_POST2, POST)
                    .build()
                    .followOneRedirect()
                    .processSamlResponse(POST)
                        .transformObject(this::extractNameIdAndSessionIndexAndTerminate)
                    .build()
                    .execute();

            // We need new SamlClient so that logout is not done using cookie -> frontchannel logout
            new SamlClientBuilder()
                    // Initiate logout as SAML_CLIENT_ID_SALES_POST2 and expect backchannel call to SAML_CLIENT_ID_SALES_POST
                    .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST2, POST)
                        .nameId(nameIdRef::get)
                        .sessionIndex(sessionIndexRef::get)
                    .build()
                    .executeAndTransform(r -> {
                        SAMLDocumentHolder saml2ObjectHolder = POST.extractResponse(r);
                        assertThat(saml2ObjectHolder.getSamlObject(), isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));

                        return null;
                    });



            // Check whether logoutReceiver contains correct LogoutRequest
            await()
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .atMost(1, TimeUnit.MINUTES)
                    .until(backchannelLogoutReceiver::isMessageReceived);

            assertThat(backchannelLogoutReceiver.isMessageReceived(), is(true));
            SAMLDocumentHolder message = backchannelLogoutReceiver.getSamlDocumentHolder();

            assertThat(message.getSamlObject(), isSamlLogoutRequest(backchannelLogoutReceiver.getUrl()));
        } catch (Exception e) {
            throw new RuntimeException("Cannot run SamlMessageReceiver", e);
        }
    }

    @Test
    public void testArtifactResolveWithWrongIssuerFails() {
        getCleanup()
            .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                    .setAttribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true")
                    .setAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "http://url")
                    .update()
            );

        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST)
                .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri()).build()
                .login().user(bburkeUser).build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST2).build() // Wrong issuer
                .execute(r -> assertThat(r, bodyHC(containsString(JBossSAMLURIConstants.STATUS_REQUEST_DENIED.get()))));
    }

    @AuthServerContainerExclude(AuthServerContainerExclude.AuthServer.REMOTE) // Won't work with openshift, because openshift wouldn't see ArtifactResolutionService
    @Test
    public void testSessionStateDuringArtifactBindingLogoutWithOneClient() {
        ClientRepresentation salesRep = adminClient.realm(REALM_NAME).clients().findByClientId(SAML_CLIENT_ID_SALES_POST).get(0);
        final String clientId = salesRep.getId();

        getCleanup()
            .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                        .setAttribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true")
                        .setAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE, "http://url")
                        .setFrontchannelLogout(true)
                        .update()
            );

        AtomicReference<String> userSessionId = new AtomicReference<>();

        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST)
                .build()
                .login().user(bburkeUser)
                .build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST)
                    .setBeforeStepChecks(new SessionStateChecker(testingClient.server())
                        .storeUserSessionId(userSessionId)
                        .expectedState(UserSessionModel.State.LOGGED_IN)
                        .expectedClientSession(clientId)
                        .consumeUserSession(userSessionModel -> assertThat(userSessionModel, notNullValue()))
                        .consumeClientSession(clientId, userSessionModel -> assertThat(userSessionModel, notNullValue())))
                    .build()
                .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, POST).build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST)
                    .setBeforeStepChecks(new SessionStateChecker(testingClient.server())
                        .expectedUserSession(userSessionId)
                        .expectedState(UserSessionModel.State.LOGGED_OUT_UNCONFIRMED)
                        .expectedNumberOfClientSessions(1)
                        .expectedAction(clientId, CommonClientSessionModel.Action.LOGGING_OUT))
                    .setAfterStepChecks(new SessionStateChecker(testingClient.server())
                        .consumeUserSession(userSessionModel -> assertThat(userSessionModel, nullValue()))
                        .setUserSessionProvider(session -> userSessionId.get()))
                    .build()
                .doNotFollowRedirects().executeAndTransform(this::getArtifactResponse);

        assertThat(response.getSamlObject(), instanceOf(ArtifactResponseType.class));
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertThat(artifactResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(artifactResponse.getSignature(), nullValue());
        assertThat(artifactResponse.getAny(), not(instanceOf(ResponseType.class)));
        assertThat(artifactResponse.getAny(), not(instanceOf(ArtifactResponseType.class)));
        assertThat(artifactResponse.getAny(), not(instanceOf(NameIDMappingResponseType.class)));
        assertThat(artifactResponse.getAny(), instanceOf(StatusResponseType.class));
        StatusResponseType samlResponse = (StatusResponseType)artifactResponse.getAny();
        assertThat(samlResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
    }

    @AuthServerContainerExclude(AuthServerContainerExclude.AuthServer.REMOTE) // Won't work with openshift, because openshift wouldn't see ArtifactResolutionService
    @Test
    public void testSessionStateDuringArtifactBindingLogoutWithMoreFrontChannelClients() {
        getCleanup()
            .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                    .setAttribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true")
                    .setAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE, "http://url")
                    .setFrontchannelLogout(true)
                    .update()
            )
            .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST2)
                    .setAttribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true")
                    .setAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE, "http://url")
                    .setFrontchannelLogout(true)
                    .update()
            );

        ClientRepresentation salesRep = adminClient.realm(REALM_NAME).clients().findByClientId(SAML_CLIENT_ID_SALES_POST).get(0);
        final String salesRepId = salesRep.getId();

        ClientRepresentation salesRep2 = adminClient.realm(REALM_NAME).clients().findByClientId(SAML_CLIENT_ID_SALES_POST2).get(0);
        final String salesRep2Id = salesRep2.getId();

        final AtomicReference<String> userSessionId = new AtomicReference<>();

        SAMLDocumentHolder response = new SamlClientBuilder()
                // Login first sales_post2 and resolve artifact
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST2, SAML_ASSERTION_CONSUMER_URL_SALES_POST2, REDIRECT)
                    .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri()).build()
                .login().user(bburkeUser).build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST2)
                    .setBeforeStepChecks(new SessionStateChecker(testingClient.server())
                        .storeUserSessionId(userSessionId)
                        .expectedClientSession(salesRep2Id)
                        .expectedState(UserSessionModel.State.LOGGED_IN)
                        .expectedNumberOfClientSessions(1)
                        .consumeUserSession(userSessionModel -> assertThat(userSessionModel, notNullValue()))
                        .consumeClientSession(salesRep2Id, clientSession -> assertThat(clientSession, notNullValue())))
                    .verifyRedirect(true)
                .build()   // This is a formal step

                // Login second sales_post and resolved artifact, no login should be needed as user is already logged in
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST, REDIRECT)
                    .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri())
                .build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST)
                    .setBeforeStepChecks(new SessionStateChecker(testingClient.server())
                        .expectedUserSession(userSessionId)
                        .expectedState(UserSessionModel.State.LOGGED_IN)
                        .expectedClientSession(salesRepId)
                        .expectedNumberOfClientSessions(2)
                        .expectedAction(salesRep2Id, null)
                        .expectedAction(salesRepId, null))
                    .verifyRedirect(true)
                .build()

                // Initiate logout from sales_post2
                .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST2, REDIRECT)
                .build()

                // Since sales_post uses frontchannel logout, keycloak should send LogoutRequest to sales_post first
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST)
                    .setBeforeStepChecks(new SessionStateChecker(testingClient.server())
                        .expectedUserSession(userSessionId)
                        .expectedState(UserSessionModel.State.LOGGING_OUT)
                        .expectedClientSession(salesRepId)
                        .expectedNumberOfClientSessions(2)
                        .expectedAction(salesRepId, CommonClientSessionModel.Action.LOGGING_OUT)
                        .expectedAction(salesRep2Id, CommonClientSessionModel.Action.LOGGING_OUT))
                    .setAfterStepChecks(new SessionStateChecker(testingClient.server())
                        .setUserSessionProvider(session -> userSessionId.get())
                        .expectedState(UserSessionModel.State.LOGGING_OUT)
                        .expectedNumberOfClientSessions(2)
                        .expectedAction(salesRepId, CommonClientSessionModel.Action.LOGGED_OUT)
                        .expectedAction(salesRep2Id, CommonClientSessionModel.Action.LOGGING_OUT))
                    .verifyRedirect(true)
                    .build()
                .doNotFollowRedirects()

                // Respond with LogoutResponse so that logout flow can continue with logging out client2
                .processSamlResponse(ARTIFACT_RESPONSE)
                    .transformDocument(doc -> {
                        // Send LogoutResponse
                        SAML2Object so = (SAML2Object) SAMLParser.getInstance().parse(new DOMSource(doc));
                        return new SAML2LogoutResponseBuilder()
                                .destination(getAuthServerSamlEndpoint(REALM_NAME).toString())
                                .issuer(SAML_CLIENT_ID_SALES_POST)
                                .logoutRequestID(((LogoutRequestType) so).getID())
                                .buildDocument();
                    })
                    .targetBinding(REDIRECT)
                    .targetAttributeSamlResponse()
                    .targetUri(getAuthServerSamlEndpoint(REALM_NAME))
                    .build()

                // Now Keycloak should finish logout process so it should respond with LogoutResponse
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST2)
                    .verifyRedirect(true)
                    .setBeforeStepChecks(new SessionStateChecker(testingClient.server())
                        .expectedUserSession(userSessionId)
                        .expectedClientSession(salesRep2Id)
                        .expectedState(UserSessionModel.State.LOGGED_OUT_UNCONFIRMED)
                        .expectedNumberOfClientSessions(2)
                        .expectedAction(salesRepId, CommonClientSessionModel.Action.LOGGED_OUT)
                        .expectedAction(salesRep2Id, CommonClientSessionModel.Action.LOGGING_OUT))
                    .setAfterStepChecks(new SessionStateChecker(testingClient.server())
                        .consumeUserSession(userSessionModel -> assertThat(userSessionModel, nullValue()))
                        .setUserSessionProvider(session -> userSessionId.get()))
                    .build()
                .executeAndTransform(this::getArtifactResponse);

        assertThat(response.getSamlObject(), instanceOf(ArtifactResponseType.class));
        ArtifactResponseType artifactResponse = (ArtifactResponseType)response.getSamlObject();
        assertThat(artifactResponse.getSignature(), nullValue());
        assertThat(artifactResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(artifactResponse.getAny(), instanceOf(StatusResponseType.class));
    }
    
    @Test
    public void testArtifactBindingIsNotUsedForLogoutWhenLogoutUrlNotSetRedirect() {
        getCleanup()
                .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                        .setAttribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true")
                        .setAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, "http://url")
                        .setFrontchannelLogout(true)
                        .update()
                );

        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, REDIRECT)
                .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri())
                .build()
                .login().user(bburkeUser).build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST).verifyRedirect(true).build()
                .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, REDIRECT).build()
                .doNotFollowRedirects()
                .executeAndTransform(REDIRECT::extractResponse);

        assertThat(response.getSamlObject(), instanceOf(StatusResponseType.class));
        StatusResponseType logoutResponse = (StatusResponseType)response.getSamlObject();
        assertThat(logoutResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(logoutResponse.getSignature(), nullValue());
        assertThat(logoutResponse, not(instanceOf(ResponseType.class)));
        assertThat(logoutResponse, not(instanceOf(ArtifactResponseType.class)));
        assertThat(logoutResponse, not(instanceOf(NameIDMappingResponseType.class)));
        assertThat(logoutResponse, instanceOf(StatusResponseType.class));
    }

    @Test
    public void testArtifactBindingIsNotUsedForLogoutWhenLogoutUrlNotSetPostTest() {
        getCleanup()
                .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                        .setAttribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true")
                        .setAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "http://url")
                        .setFrontchannelLogout(true)
                        .update()
                );

        SAMLDocumentHolder response = new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST)
                .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri())
                .build()
                .login().user(bburkeUser).build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST).build()
                .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, POST).build()
                .doNotFollowRedirects()
                .executeAndTransform(POST::extractResponse);

        assertThat(response.getSamlObject(), instanceOf(StatusResponseType.class));
        StatusResponseType logoutResponse = (StatusResponseType)response.getSamlObject();
        assertThat(logoutResponse, isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(logoutResponse.getSignature(), nullValue());
        assertThat(logoutResponse, not(instanceOf(ResponseType.class)));
        assertThat(logoutResponse, not(instanceOf(ArtifactResponseType.class)));
        assertThat(logoutResponse, not(instanceOf(NameIDMappingResponseType.class)));
        assertThat(logoutResponse, instanceOf(StatusResponseType.class));
    }

    private SAMLDocumentHolder getArtifactResponse(CloseableHttpResponse response) throws IOException, ParsingException, ProcessingException {
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

    /************************ IMPORT CLIENT TESTS ************************/
    @Test
    public void testImportClientArtifactResolutionSingleServices() {
        Document doc = IOUtil.loadXML(ArtifactBindingTest.class.getResourceAsStream("/saml/sp-metadata-artifact-simple.xml"));
        ClientRepresentation clientRep = adminClient.realm(REALM_NAME).convertClientDescription(IOUtil.documentToString(doc));
        assertThat(clientRep.getAttributes().get(SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE), is("https://test.keycloak.com/auth/login/epd/callback/soap"));
        assertThat(clientRep.getAttributes().get(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE), is("https://test.keycloak.com/auth/login/epd/callback/http-artifact"));
    }

    @Test
    public void testImportClientMultipleServices() {
        Document doc = IOUtil.loadXML(ArtifactBindingTest.class.getResourceAsStream("/saml/sp-metadata-artifact-multiple.xml"));
        ClientRepresentation clientRep = adminClient.realm(REALM_NAME).convertClientDescription(IOUtil.documentToString(doc));
        assertThat(clientRep.getAttributes().get(SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE), is("https://test.keycloak.com/auth/login/epd/callback/soap-1"));
        assertThat(clientRep.getAttributes().get(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE), Matchers.startsWith("https://test.keycloak.com/auth/login/epd/callback/http-artifact"));
    }

    @Test
    public void testImportClientMultipleServicesWithDefault() {
        Document doc = IOUtil.loadXML(ArtifactBindingTest.class.getResourceAsStream("/saml/sp-metadata-artifact-multiple-default.xml"));
        ClientRepresentation clientRep = adminClient.realm(REALM_NAME).convertClientDescription(IOUtil.documentToString(doc));
        assertThat(clientRep.getAttributes().get(SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE), is("https://test.keycloak.com/auth/login/epd/callback/soap-9"));
        assertThat(clientRep.getAttributes().get(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE), Matchers.startsWith("https://test.keycloak.com/auth/login/epd/callback/http-artifact"));
    }

    @Test
    public void testSPMetadataArtifactBindingNotUsedForLogout() throws ParsingException, URISyntaxException {

        getCleanup()
                .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                        .setAttribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true")
                        .setAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE, "http://url.artifact.test")
                        .setAdminUrl("http://admin.url.test")
                        .update()
                );
        SPSSODescriptorType spDescriptor = getSPInstallationDescriptor(adminClient.realm(REALM_NAME).clients(), SAML_CLIENT_ID_SALES_POST);
        assertThat(spDescriptor.getAssertionConsumerService().get(0).getBinding(), is(equalTo(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri())));
        assertThat(spDescriptor.getAssertionConsumerService().get(0).getLocation(), is(equalTo(new URI("http://url.artifact.test"))));
        
        assertThat(spDescriptor.getSingleLogoutService().get(0).getBinding(), is(equalTo(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri())));
        assertThat(spDescriptor.getSingleLogoutService().get(0).getLocation(), is(equalTo(new URI("http://admin.url.test"))));
    }

    @Test
    public void testSPMetadataArtifactBindingUsedForLogout() throws ParsingException, URISyntaxException {
        getCleanup()
                .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                        .setAttribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true")
                        .setAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE, "http://url.artifact.test")
                        .setAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE, "http://url.artifact.test")
                        .setAdminUrl("http://admin.url.test")
                        .update()
                );

        SPSSODescriptorType spDescriptor = getSPInstallationDescriptor(adminClient.realm(REALM_NAME).clients(), SAML_CLIENT_ID_SALES_POST);
        assertThat(spDescriptor.getAssertionConsumerService().get(0).getBinding(), is(equalTo(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri())));
        assertThat(spDescriptor.getAssertionConsumerService().get(0).getLocation(), is(equalTo(new URI("http://url.artifact.test"))));

        assertThat(spDescriptor.getSingleLogoutService().get(0).getBinding(), is(equalTo(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri())));
        assertThat(spDescriptor.getSingleLogoutService().get(0).getLocation(), is(equalTo(new URI("http://url.artifact.test"))));
    }

    @Test
    public void testArtifactBindingIdentifierChangedWhenClientIdChanged() throws IOException {
        ClientRepresentation clientRepresentation = adminClient.realm(REALM_NAME)
                .clients()
                .findByClientId(SAML_CLIENT_ID_SALES_POST)
                .get(0);

        String oldIdentifier = clientRepresentation.getAttributes().get(SamlConfigAttributes.SAML_ARTIFACT_BINDING_IDENTIFIER);
        assertThat(oldIdentifier, notNullValue());

        final String newClientId = "new_client_id";

        try (ClientAttributeUpdater cau = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
            .setClientId(newClientId)
             .update()
        ) {
            clientRepresentation = adminClient.realm(REALM_NAME)
                    .clients()
                    .findByClientId(newClientId)
                    .get(0);

            String identifier = clientRepresentation.getAttributes().get(SamlConfigAttributes.SAML_ARTIFACT_BINDING_IDENTIFIER);

            assertThat(identifier, not(equalTo(oldIdentifier)));
            assertThat(identifier, equalTo(ArtifactBindingUtils.computeArtifactBindingIdentifierString(newClientId)));
        }

        clientRepresentation = adminClient.realm(REALM_NAME)
                .clients()
                .findByClientId(SAML_CLIENT_ID_SALES_POST)
                .get(0);

        assertThat(clientRepresentation.getAttributes().get(SamlConfigAttributes.SAML_ARTIFACT_BINDING_IDENTIFIER), equalTo(oldIdentifier));
    }

}
