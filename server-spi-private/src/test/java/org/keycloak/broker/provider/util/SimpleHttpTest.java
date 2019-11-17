package org.keycloak.broker.provider.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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
}