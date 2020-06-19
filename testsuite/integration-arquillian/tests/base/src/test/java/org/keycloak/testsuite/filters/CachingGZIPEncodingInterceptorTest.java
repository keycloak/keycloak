package org.keycloak.testsuite.filters;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.Version;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;

import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

@AuthServerContainerExclude(AuthServer.REMOTE)
public class CachingGZIPEncodingInterceptorTest extends AbstractTestRealmKeycloakTest {
    public static final String WELCOME_CSS = "%s/auth/resources/%s/welcome/keycloak/css/welcome.css";

    @ArquillianResource
    protected SuiteContext suiteContext;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void compressionVsUncompressed() throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build();
             CloseableHttpClient withoutCompressionHttpClient = HttpClientBuilder.create()
                     .disableContentCompression()
                     .build()) {
            final String resource = String.format(WELCOME_CSS,
                    suiteContext.getAuthServerInfo().getContextRoot(), Version.RESOURCES_VERSION);

            final HttpGet httpGet = new HttpGet(resource);
            final GzipDecompressingEntity entity = new GzipDecompressingEntity(httpClient.execute(httpGet).getEntity());

            final String decompressedOutput = IOUtils.toString(entity.getContent(), Charset.defaultCharset());
            final String disabledCompressionOutput = SimpleHttp.doGet(resource, withoutCompressionHttpClient).asString();

            assertEquals(decompressedOutput, disabledCompressionOutput);
        }
    }

    @Test
    public void compareDynamicContentToCached() throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            final String resource = String.format(WELCOME_CSS,
                    suiteContext.getAuthServerInfo().getContextRoot(), Version.RESOURCES_VERSION);
            final HttpGet httpGet = new HttpGet(resource);

            //First request will generate the zip stream on the fly
            final GzipDecompressingEntity entity = new GzipDecompressingEntity(httpClient.execute(httpGet).getEntity());
            final String dynamic = IOUtils.toString(entity.getContent(), Charset.defaultCharset());

            //Second request will use the cached zip file
            final GzipDecompressingEntity entityCached = new GzipDecompressingEntity(httpClient.execute(httpGet).getEntity());
            final String staticContent = IOUtils.toString(entityCached.getContent(), Charset.defaultCharset());

            assertEquals(dynamic, staticContent);
        }
    }
}
