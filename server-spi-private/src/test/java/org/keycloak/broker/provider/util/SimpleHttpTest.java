package org.keycloak.broker.provider.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.keycloak.common.util.StreamUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:viruszold@gmail.com">Andrey Khlebnikov</a>
 * @version $Revision: 1 $
 */
public final class SimpleHttpTest {

    @RunWith(Parameterized.class)
    public static final class ResponseConsideringCharsetTest {
        private final StringEntity entity;
        private final String original;
        private final boolean success;

        public ResponseConsideringCharsetTest(String original, Charset charset, boolean success) {
            this.original = original;
            this.success = success;
            this.entity = new StringEntity(original, ContentType.create("text/plain", charset));
        }

        @Parameters(name = "{index}: withCharset({0}, {1}) = {2}")
        public static Collection<Object[]> entities() {
            return Arrays.asList(new Object[][]{
                    {"English", StandardCharsets.UTF_8, true},
                    {"Русский", StandardCharsets.UTF_8, true},
                    {"Русский", StandardCharsets.ISO_8859_1, false},
                    {"Русский", null, false},
            });
        }

        @Test
        public void withCharset() throws IOException {
            HttpResponse httpResponse = createBasicResponse(entity);
            SimpleHttp.Response response = new SimpleHttp.Response(httpResponse);
            if (success) {
                assertEquals(original, response.asString());
            } else {
                assertNotEquals(original, response.asString());
            }
        }

        private HttpResponse createBasicResponse(HttpEntity entity) {
            BasicHttpResponse response = new BasicHttpResponse(new HttpVersion(1, 1), 200, "OK");
            response.setEntity(entity);
            return response;
        }

    }

    @RunWith(Parameterized.class)
    public static final class RequestConsideringEncodingTest {
        private String value;

        public RequestConsideringEncodingTest(String value) {
            this.value = value;
        }

        @Parameters(name = "{index}: requestWithEncoding({0})")
        public static Collection<Object[]> entities() {
            return Arrays.asList(new Object[][] { { "English" }, { "Русский" }, { "GermanÜmläütß" } });
        }

        @Test
        public void requestWithEncoding() throws IOException {
            HttpClientMock client = new HttpClientMock();
            SimpleHttp.doPost("", client).json(new DummyEntity(value)).asResponse();
            assertEquals("{\"value\":\"" + value + "\"}", client.data);
        }

        public static final class DummyEntity {
            public String value;
            public DummyEntity(String value) {
                this.value = value;
            }
        }

        /**
         * As no mocking framework is wanted, this is done the good old way.
         */
        public static final class HttpClientMock implements HttpClient {

            String data;

            @Override
            public HttpParams getParams() {
                fail(); return null;
            }

            @Override
            public ClientConnectionManager getConnectionManager() {
                fail(); return null;
            }

            @Override
            public HttpResponse execute(HttpUriRequest paramHttpUriRequest) throws IOException, ClientProtocolException {
                HttpPost post = (HttpPost) paramHttpUriRequest;
                data = StreamUtil.readString(post.getEntity().getContent());
                return null;
            }

            @Override
            public HttpResponse execute(HttpUriRequest paramHttpUriRequest, HttpContext paramHttpContext)
                    throws IOException, ClientProtocolException {
                fail(); return null;
            }

            @Override
            public HttpResponse execute(HttpHost paramHttpHost, HttpRequest paramHttpRequest) throws IOException, ClientProtocolException {
                fail(); return null;
            }

            @Override
            public HttpResponse execute(HttpHost paramHttpHost, HttpRequest paramHttpRequest, HttpContext paramHttpContext)
                    throws IOException, ClientProtocolException {
                fail(); return null;
            }

            @Override
            public <T> T execute(HttpUriRequest paramHttpUriRequest, ResponseHandler<? extends T> paramResponseHandler)
                    throws IOException, ClientProtocolException {
                fail(); return null;
            }

            @Override
            public <T> T execute(HttpUriRequest paramHttpUriRequest, ResponseHandler<? extends T> paramResponseHandler,
                    HttpContext paramHttpContext) throws IOException, ClientProtocolException {
                fail(); return null;
            }

            @Override
            public <T> T execute(HttpHost paramHttpHost, HttpRequest paramHttpRequest, ResponseHandler<? extends T> paramResponseHandler)
                    throws IOException, ClientProtocolException {
                fail(); return null;
            }

            @Override
            public <T> T execute(HttpHost paramHttpHost, HttpRequest paramHttpRequest, ResponseHandler<? extends T> paramResponseHandler,
                    HttpContext paramHttpContext) throws IOException, ClientProtocolException {
                fail(); return null;
            }

        }
    }

}