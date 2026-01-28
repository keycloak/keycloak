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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import org.keycloak.testsuite.auth.page.login.PageWithLoginUrl;
import org.keycloak.testsuite.page.AbstractPage;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.awaitility.core.ThrowingRunnable;
import org.junit.Assert;
import org.openqa.selenium.WebDriver;

import static org.keycloak.testsuite.util.ServerURLs.removeDefaultPorts;
import static org.keycloak.testsuite.util.URLUtils.currentUrlDoesntStartWith;
import static org.keycloak.testsuite.util.URLUtils.currentUrlEquals;
import static org.keycloak.testsuite.util.URLUtils.currentUrlStartsWith;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author tkyjovsk
 */
public class URLAssert {

    public static void assertCurrentUrlEquals(final AbstractPage page, WebDriver driver) {
        assertCurrentUrlEquals(page.toString(), driver);
    }

    public static void assertCurrentUrlEquals(final String url, WebDriver driver) {
        DroneUtils.addWebDriver(driver);
        assertCurrentUrlEquals(url);
        DroneUtils.removeWebDriver();
    }

    public static void assertCurrentUrlEquals(final AbstractPage page) {
        assertCurrentUrlEquals(page.toString());
    }

    public static void assertCurrentUrlEquals(final URI url) {
        assertCurrentUrlEquals(url.toASCIIString());
    }

    public static void assertCurrentUrlEquals(final String url) {
        awaitUntilAsserted(() -> {
            assertTrue("Expected URL: " + url + " ; actual: " + DroneUtils.getCurrentDriver().getCurrentUrl(),
                    currentUrlEquals(url));
        });
    }

    public static void assertCurrentUrlStartsWith(final AbstractPage page, WebDriver driver) {
        assertCurrentUrlStartsWith(page.toString(), driver);
    }

    public static void assertCurrentUrlStartsWith(final String url, WebDriver driver) {
        DroneUtils.addWebDriver(driver);
        assertCurrentUrlStartsWith(url);
        DroneUtils.removeWebDriver();
    }

   public static void assertCurrentUrlStartsWith(final AbstractPage page) {
        assertCurrentUrlStartsWith(page.toString());
   }

    public static void assertCurrentUrlStartsWith(final String url){
        awaitUntilAsserted(() -> {
            assertTrue("URL expected to begin with: " + removeDefaultPorts(url) + " ; actual URL: " + DroneUtils.getCurrentDriver().getCurrentUrl(),
                    currentUrlStartsWith(removeDefaultPorts(url)));
        });
    }

    public static void assertCurrentUrlDoesntStartWith(final AbstractPage page, WebDriver driver) {
        assertCurrentUrlDoesntStartWith(page.toString(), driver);
    }

    public static void assertCurrentUrlDoesntStartWith(final String url, WebDriver driver) {
        DroneUtils.addWebDriver(driver);
        awaitUntilAsserted(() -> {
            assertCurrentUrlDoesntStartWith(url);
        });
        DroneUtils.removeWebDriver();
    }

    public static void assertCurrentUrlDoesntStartWith(AbstractPage page) {
        assertCurrentUrlDoesntStartWith(page.toString());
    }

    public static void assertCurrentUrlDoesntStartWith(final String url) {
        awaitUntilAsserted(() -> {
            assertTrue("URL expected NOT to begin with: " + url + " ; actual URL: " + DroneUtils.getCurrentDriver().getCurrentUrl(),
                    currentUrlDoesntStartWith(url));
        });
    }


    public static void assertCurrentUrlStartsWithLoginUrlOf(final PageWithLoginUrl page, WebDriver driver) {
        assertCurrentUrlStartsWithLoginUrlOf(page.getOIDCLoginUrl().toString(), driver);
    }

    public static void assertCurrentUrlStartsWithLoginUrlOf(final String url, WebDriver driver) {
        DroneUtils.addWebDriver(driver);
        assertCurrentUrlStartsWithLoginUrlOf(url);
        DroneUtils.removeWebDriver();
    }

    public static void assertCurrentUrlStartsWithLoginUrlOf(final PageWithLoginUrl page) {
        assertCurrentUrlStartsWithLoginUrlOf(page.getOIDCLoginUrl().toString());
    }

    public static void assertCurrentUrlStartsWithLoginUrlOf(final String url) {
        assertCurrentUrlStartsWith(url);
    }


    public interface AssertResponseHandler {
        void assertResponse(CloseableHttpResponse response) throws IOException;
    }

    public static abstract class AssertJSONResponseHandler implements AssertResponseHandler {

        @Override
        public void assertResponse(CloseableHttpResponse response) throws IOException {
            HttpEntity entity = response.getEntity();
            Header contentType = entity.getContentType();
            Assert.assertTrue(contentType.getValue().startsWith("application/json"));

            char [] buf = new char[8192];
            StringWriter out = new StringWriter();
            Reader in = new InputStreamReader(entity.getContent(), Charset.forName("utf-8"));
            int rc;
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

    private static void awaitUntilAsserted(ThrowingRunnable r) {
        await()
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .pollInSameThread() // to ensure that drones are accessible
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(r);
    }

}
