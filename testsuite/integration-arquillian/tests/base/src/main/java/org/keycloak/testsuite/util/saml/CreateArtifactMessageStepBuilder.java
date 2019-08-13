package org.keycloak.testsuite.util.saml;

import com.google.common.base.Charsets;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.logging.Logger;
import org.keycloak.protocol.saml.ArtifactResolver;
import org.keycloak.protocol.saml.ArtifactResolverProcessingException;
import org.keycloak.protocol.saml.DefaultSamlArtifactResolver;
import org.keycloak.protocol.saml.SamlProtocolUtils;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

public class CreateArtifactMessageStepBuilder implements SamlClient.Step {

    private static final Logger LOG = Logger.getLogger(CreateArtifactMessageStepBuilder.class);

    private final URI authServerSamlUrl;
    private final SamlClient.Binding requestBinding;
    private final SamlClientBuilder clientBuilder;
    private final String issuer;
    private String lastArtifact;
    private ArtifactResolver artifactResolver = new DefaultSamlArtifactResolver();

    public CreateArtifactMessageStepBuilder(URI authServerSamlUrl, String issuer, SamlClient.Binding requestBinding, SamlClientBuilder clientBuilder) {
        this.authServerSamlUrl = authServerSamlUrl;
        this.requestBinding = requestBinding;
        this.clientBuilder = clientBuilder;
        this.issuer = issuer;
    }

    @Override
    public HttpUriRequest perform(CloseableHttpClient client, URI currentURI, CloseableHttpResponse currentResponse, HttpClientContext context) throws Exception {
        DefaultSamlArtifactResolver artifactResolver = new DefaultSamlArtifactResolver();
        lastArtifact = artifactResolver.createArtifact(issuer);
        if (SamlClient.Binding.POST == requestBinding) {
            return sendArtifactMessagePost();
        }
        return sendArtifactMessageRedirect();
    }

    private HttpUriRequest sendArtifactMessageRedirect() throws IOException, ProcessingException, URISyntaxException {
        URIBuilder builder = new URIBuilder(authServerSamlUrl)
                .setParameter(GeneralConstants.SAML_ARTIFACT_KEY, lastArtifact);

        LOG.infof("Sending GET request with artifact %s", lastArtifact);
        return new HttpGet(builder.build());
    }

    private HttpUriRequest sendArtifactMessagePost() throws IOException, ProcessingException {
        HttpPost post = new HttpPost(authServerSamlUrl);
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(GeneralConstants.SAML_ARTIFACT_KEY, lastArtifact));
        LOG.infof("Sending POST request with artifact %s", lastArtifact);

        UrlEncodedFormEntity formEntity;
        try {
            formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        post.setEntity(formEntity);

        return post;
    }

    public SamlClientBuilder build() {
        return clientBuilder;
    }

    public String getLastArtifact() {
        return lastArtifact;
    }
}
