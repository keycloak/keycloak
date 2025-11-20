package org.keycloak.testsuite.util.saml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.stream.XMLStreamWriter;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.common.util.KeyUtils;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.ArtifactResolveType;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlService;
import org.keycloak.protocol.saml.profile.util.Soap;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLRequestWriter;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * This builder allows the SamlClient to handle a redirect or a POSTed form which contains an artifact (SAMLart)
 */
public class HandleArtifactStepBuilder extends SamlDocumentStepBuilder<ArtifactResolveType, HandleArtifactStepBuilder> implements StepWithCheckers {

    private String signingPrivateKeyPem;
    private String signingPublicKeyPem;
    private String id = IDGenerator.create("ID_");
    private String issuer;
    private final URI authServerSamlUrl;
    private boolean verifyRedirect;
    private HttpPost replayPostMessage;
    private boolean replayPost;
    private boolean replayArtifact;
    private AtomicReference<String> providedArtifact;
    private AtomicReference<String> storeArtifact;

    private Runnable beforeStepChecker;
    private Runnable afterStepChecker;

    private final static Pattern artifactPattern = Pattern.compile("NAME=\"SAMLart\" VALUE=\"([A-Za-z0-9+=/]*)\"");

    /**
     * Standard constructor
     *  @param authServerSamlUrl the url of the IdP
     * @param issuer the value for the issuer
     * @param clientBuilder the current clientBuilder
     */
    public HandleArtifactStepBuilder(URI authServerSamlUrl, String issuer, SamlClientBuilder clientBuilder) {
        super(clientBuilder);
        this.issuer = issuer;
        this.authServerSamlUrl = authServerSamlUrl.toString().endsWith(SamlService.ARTIFACT_RESOLUTION_SERVICE_PATH) ? authServerSamlUrl : UriBuilder.fromUri(authServerSamlUrl).path(SamlService.ARTIFACT_RESOLUTION_SERVICE_PATH).build();
        verifyRedirect = false;
    }

    /**
     * Builder method. Calling this method with the public and private key will ensure that the generated ArifactResolve is signed
     * @param signingPrivateKeyPem the pem containing the client's private key
     * @param signingPublicKeyPem the pem containing the client's public key
     * @return this HandleArtifactStepBuilder
     */
    public HandleArtifactStepBuilder signWith(String signingPrivateKeyPem, String signingPublicKeyPem) {
        this.signingPrivateKeyPem = signingPrivateKeyPem;
        this.signingPublicKeyPem = signingPublicKeyPem;
        return this;
    }
    
    public HandleArtifactStepBuilder issuer(String issuer) {
        this.issuer = issuer;
        return this;
    }
    
    public HandleArtifactStepBuilder setBeforeStepChecks(Runnable checker) {
        this.beforeStepChecker = checker;
        return this;
    }

    public HandleArtifactStepBuilder setAfterStepChecks(Runnable checker) {
        this.afterStepChecker = checker;
        return this;
    }
    
    /**
     * Builder method. Calling this method with "true" will add an assertion to verify that the returned method was a redirect
     * @param verifyRedirect set true to verify redirect
     * @return this HandleArtifactStepBuilder
     */
    public HandleArtifactStepBuilder verifyRedirect(boolean verifyRedirect) {
        this.verifyRedirect = verifyRedirect;
        return this;
    }

    /**
     * Builder method. Call this method with "true" to make sure that the second time "perform" is called, it is a replay of the first time.
     * This is specifically to test that the artifact is consumed on the IdP side once called.
     * @param mustReplayPost set true to replay on the second call
     * @return this HandleArtifactStepBuilder
     */
    public HandleArtifactStepBuilder replayPost(boolean mustReplayPost) {
        this.replayPost = mustReplayPost;
        return this;
    }

    public HandleArtifactStepBuilder storeArtifact(AtomicReference<String> storeArtifact) {
        this.storeArtifact = storeArtifact;
        return this;
    }
    
    public HandleArtifactStepBuilder useArtifact(AtomicReference<String> artifact) {
        this.providedArtifact = artifact;
        return this;
    }

    /**
     * Builder method. Calling this method will set the ArtifactResolve from the standard generated to a specific id
     * @param id the value to which to set the ArtifactResolve's id
     * @return this HandleArtifactStepBuilder
     */
    public HandleArtifactStepBuilder setArtifactResolveId(String id){
        this.id = id;
        return this;
    }

    /**
     * Main method. Can read a response with an artifact (redirect or post) and return a POSTed SOAP message containing
     * the ArtifactResolve message. The behaviour changes depending on what builder methods were called.
     *
     * @param client The current http client
     * @param currentURI the current uri
     * @param currentResponse the current response from the IdP
     * @param context the current http context
     * @return a POSTed SOAP message containing the ArtifactResolve message
     * @throws Exception
     */
    @Override
    public HttpUriRequest perform(CloseableHttpClient client, URI currentURI, CloseableHttpResponse currentResponse, HttpClientContext context) throws Exception {

        if (replayPost && replayPostMessage != null) {
            return replayPostMessage;
        }

        ArtifactResolveType artifactResolve = new ArtifactResolveType(id,
                XMLTimeUtil.getIssueInstant());
        NameIDType nameIDType = new NameIDType();
        nameIDType.setValue(issuer);
        artifactResolve.setIssuer(nameIDType);
        String artifact = getArtifactFromResponse(currentResponse);
        if (storeArtifact != null) storeArtifact.set(artifact);
        artifactResolve.setArtifact(artifact);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLStreamWriter xmlStreamWriter = StaxUtil.getXMLStreamWriter(bos);
        new SAMLRequestWriter(xmlStreamWriter).write(artifactResolve);
        Document doc = DocumentUtil.getDocument(new ByteArrayInputStream(bos.toByteArray()));

        BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder();

        if (signingPrivateKeyPem != null && signingPublicKeyPem != null) {
            PrivateKey privateKey = org.keycloak.testsuite.util.KeyUtils.privateKeyFromString(signingPrivateKeyPem);
            PublicKey publicKey = org.keycloak.testsuite.util.KeyUtils.publicKeyFromString(signingPublicKeyPem);
            binding
                    .signatureAlgorithm(SignatureAlgorithm.RSA_SHA256)
                    .signWith(KeyUtils.createKeyId(privateKey), privateKey, publicKey)
                    .signDocument(doc);
        }

        String documentAsString = DocumentUtil.getDocumentAsString(doc);
        String transformed = getTransformer().transform(documentAsString);

        if (transformed == null) return null;

        if (beforeStepChecker != null && beforeStepChecker instanceof SessionStateChecker) {
            SessionStateChecker sessionStateChecker = (SessionStateChecker) beforeStepChecker;
            sessionStateChecker.setUserSessionProvider(session -> session.singleUseObjects().get(artifact).get(SamlProtocol.USER_SESSION_ID));
            sessionStateChecker.setClientSessionProvider(session -> session.singleUseObjects().get(artifact).get(SamlProtocol.CLIENT_SESSION_ID));
        }

        HttpPost post =  Soap.createMessage().addToBody(DocumentUtil.getDocument(transformed)).buildHttpPost(authServerSamlUrl);
        replayPostMessage = post;
        return post;
    }
    
    /**
     * Extracts the artifact from a response. Can handle both a Redirect and a POSTed form
     * @param currentResponse the response containing the artifact
     * @return the artifact
     * @throws IOException thrown if there'a a problem processing the response.
     */
    private String getArtifactFromResponse(CloseableHttpResponse currentResponse) throws IOException {

        if (providedArtifact != null) {
            return providedArtifact.get();
        }
        
        if (currentResponse.getFirstHeader("location") != null) {
            String location = currentResponse.getFirstHeader("location").getValue();
            List<NameValuePair> params = URLEncodedUtils.parse(URI.create(location), StandardCharsets.UTF_8);
            for (NameValuePair param : params) {
                if (GeneralConstants.SAML_ARTIFACT_KEY.equals(param.getName())) {
                    String artifact = param.getValue();
                    if (artifact != null && !artifact.isEmpty()) {
                        return artifact;
                    }
                }
            }
        }
        assertFalse(verifyRedirect);
        String form = EntityUtils.toString(currentResponse.getEntity());

        Matcher m = artifactPattern.matcher(form);
        assertTrue("Can't find artifact in " + form, m.find());
        return m.group(1);
    }

    @Override
    public Runnable getBeforeStepChecker() {
        return beforeStepChecker;
    }

    @Override
    public Runnable getAfterStepChecker() {
        return afterStepChecker;
    }
}
