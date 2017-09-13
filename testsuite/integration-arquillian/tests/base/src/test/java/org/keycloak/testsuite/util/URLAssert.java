/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.util;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.keycloak.testsuite.auth.page.login.PageWithLoginUrl;
import org.keycloak.testsuite.page.AbstractPage;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;

import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.URLUtils.currentUrlDoesntStartWith;
import static org.keycloak.testsuite.util.URLUtils.currentUrlEqual;
import static org.keycloak.testsuite.util.URLUtils.currentUrlStartWith;

/**
 *
 * @author tkyjovsk
 */
public class URLAssert {

    public static void assertCurrentUrlEquals(AbstractPage page) {
        assertCurrentUrlEquals(page.getDriver(), page);
    }

    public static void assertCurrentUrlEquals(WebDriver driver, final AbstractPage page) {
        String expected = page.toString();
        assertTrue("Expected URL: " + expected + "; actual: " + driver.getCurrentUrl(),
                currentUrlEqual(page.toString()));
    }

    public static void assertCurrentUrlEquals(WebDriver driver, final String url) {
        assertTrue("Expected URL: " + url + "; actual: " + driver.getCurrentUrl(),
                currentUrlEqual(url));
    }

    public static void assertCurrentUrlStartsWith(AbstractPage page) {
        assertCurrentUrlStartsWith(page.getDriver(), page.toString());
    }

    public static void assertCurrentUrlStartsWith(WebDriver driver, final String url) {
        assertTrue("URL expected to begin with:" + url + "; actual URL: " + driver.getCurrentUrl(),
                currentUrlStartWith(url));
    }

    public static void assertCurrentUrlDoesntStartWith(AbstractPage page) {
        assertCurrentUrlDoesntStartWith(page.getDriver(), page.toString());
    }

    public static void assertCurrentUrlDoesntStartWith(WebDriver driver, final String url) {
        assertTrue("URL expected NOT to begin with:" + url + "; actual URL: " + driver.getCurrentUrl(),
                currentUrlDoesntStartWith(url));
    }

    public static void assertCurrentUrlStartsWithLoginUrlOf(PageWithLoginUrl page) {
        assertCurrentUrlStartsWithLoginUrlOf(page.getDriver(), page);
    }

    public static void assertCurrentUrlStartsWithLoginUrlOf(WebDriver driver, PageWithLoginUrl page) {
        assertCurrentUrlStartsWith(driver, page.getOIDCLoginUrl().toString());
    }

    public static void assertGetURL(URI url, String accessToken, AssertResponseHandler handler) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", "Bearer " + accessToken);

            CloseableHttpResponse response = httpclient.execute(get);

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Response status error: " + response.getStatusLine().getStatusCode() + ": " + url);
            }

            handler.assertResponse(response);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public interface AssertResponseHandler {
        void assertResponse(CloseableHttpResponse response) throws IOException;
    }

    public static abstract class AssertJSONResponseHandler implements AssertResponseHandler {

        @Override
        public void assertResponse(CloseableHttpResponse response) throws IOException {
            HttpEntity entity = response.getEntity();
            Header contentType = entity.getContentType();
            Assert.assertEquals("application/json", contentType.getValue());

            char [] buf = new char[8192];
            StringWriter out = new StringWriter();
            Reader in = new InputStreamReader(entity.getContent(), Charset.forName("utf-8"));
            int rc = 0;
            try {
                while ((rc = in.read(buf)) != -1) {
                    out.write(buf, 0, rc);
                }
            } finally {
                try {
                    in.close();
                } catch (Exception ignored) {}

                out.close();
            }

            assertResponseBody(out.toString());
        }

        protected abstract void assertResponseBody(String body) throws IOException;
    }
}
