package org.keycloak.testsuite.saml;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.testsuite.arquillian.annotation.SetDefaultProvider;
import org.keycloak.testsuite.authentication.CustomTestingSamlArtifactResolver;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import static org.keycloak.testsuite.util.SamlClient.Binding.POST;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@SetDefaultProvider(spi = "saml-artifact-resolver", providerId = "0005")
public class ArtifactBindingCustomResolverTest extends ArtifactBindingTest {

    @Test
    @Ignore
    @Override
    public void testArtifactBindingLogoutSingleClientCheckArtifact() {}

    @Test
    @Ignore
    @Override
    public void testArtifactBindingLoginCheckArtifactWithPost() {}

    @Test
    public void testCustomArtifact() {
        AtomicReference<String> artifactReference = new AtomicReference<>();

        new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                    .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri())
                .build()
                .login().user(bburkeUser).build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST)
                    .storeArtifact(artifactReference)
                .build()
                .execute();

        String artifact = artifactReference.get();
        byte[] byteArray = Base64.getDecoder().decode(artifact);
        ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
        bis.skip(2);
        int index = bis.read();
        
        assertThat(byteArray[0], is((byte)0));
        assertThat(byteArray[1], is((byte)5));

        if (!suiteContext.getAuthServerInfo().isUndertow()) return;

        String storedResponse = CustomTestingSamlArtifactResolver.list.get(index);

        assertThat(storedResponse, notNullValue());
        assertThat(storedResponse, containsString("samlp:Response"));
    }

    @Test
    public void testArtifactDoesntContainSignature() {
        ContainerAssume.assumeAuthServerUndertow();

        AtomicReference<String> artifactReference = new AtomicReference<>();

        new SamlClientBuilder().authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_ASSERTION_AND_RESPONSE_SIG,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST_ASSERTION_AND_RESPONSE_SIG, POST)
                    .setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri())
                    .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(bburkeUser).build()
                .handleArtifact(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_ASSERTION_AND_RESPONSE_SIG)
                    .storeArtifact(artifactReference)
                    .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .execute();

        String artifact = artifactReference.get();
        byte[] byteArray = Base64.getDecoder().decode(artifact);
        ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
        bis.skip(2);
        int index = bis.read();

        assertThat(byteArray[0], is((byte)0));
        assertThat(byteArray[1], is((byte)5));

        String storedResponse = CustomTestingSamlArtifactResolver.list.get(index);

        assertThat(storedResponse, notNullValue());
        assertThat(storedResponse, containsString("samlp:Response"));
        assertThat(storedResponse, not(containsString("Signature")));
    }
}
