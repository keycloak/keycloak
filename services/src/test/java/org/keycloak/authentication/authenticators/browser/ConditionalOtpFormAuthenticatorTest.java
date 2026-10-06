package org.keycloak.authentication.authenticators.browser;

import java.lang.reflect.Method;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.http.FormPartValue;
import org.keycloak.http.HttpRequest;

import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.FORCE_OTP_FOR_HTTP_HEADER;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.SKIP_OTP_FOR_HTTP_HEADER;

public class ConditionalOtpFormAuthenticatorTest {

    private static class SimpleTestHttpRequest implements HttpRequest {
        private final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        private final boolean trusted;

        public SimpleTestHttpRequest(boolean trusted) {
            this.trusted = trusted;
        }

        public void addHeader(String name, String value) {
            headers.add(name, value);
        }

        @Override
        public HttpHeaders getHttpHeaders() {
            return new HttpHeaders() {
                @Override
                public MultivaluedMap<String, String> getRequestHeaders() {
                    return headers;
                }

                @Override
                public List<String> getRequestHeader(String name) {
                    return headers.get(name);
                }

                @Override
                public String getHeaderString(String name) {
                    return headers.getFirst(name);
                }

                @Override
                public List<MediaType> getAcceptableMediaTypes() { return Collections.emptyList(); }
                @Override
                public List<Locale> getAcceptableLanguages() { return Collections.emptyList(); }
                @Override
                public MediaType getMediaType() { return null; }
                @Override
                public Locale getLanguage() { return null; }
                @Override
                public Map<String, Cookie> getCookies() { return Collections.emptyMap(); }
                @Override
                public Date getDate() { return null; }
                @Override
                public int getLength() { return 0; }
            };
        }

        @Override public String getHttpMethod() { return "GET"; }
        @Override public MultivaluedMap<String, String> getDecodedFormParameters() { return new MultivaluedHashMap<>(); }
        @Override public MultivaluedMap<String, FormPartValue> getMultiPartFormParameters() { return new MultivaluedHashMap<>(); }
        @Override public X509Certificate[] getClientCertificateChain() { return null; }
        @Override public UriInfo getUri() { return null; }
        @Override public boolean isProxyTrusted() { return trusted; }
    }

    private Object invokeVoteForHttpHeader(ConditionalOtpFormAuthenticator authenticator, HttpRequest request, Map<String, String> config) throws Exception {
        Method method = ConditionalOtpFormAuthenticator.class.getDeclaredMethod("voteForHttpHeaderMatchesPattern", HttpRequest.class, Map.class);
        method.setAccessible(true);
        return method.invoke(authenticator, request, config);
    }

    @Test
    public void testSkipOtpIgnoredWhenProxyNotConfigured() throws Exception {
        ConditionalOtpFormAuthenticator authenticator = new ConditionalOtpFormAuthenticator();
        authenticator.setProxyConfigured(false);

        SimpleTestHttpRequest request = new SimpleTestHttpRequest(true);
        request.addHeader("X-Forwarded-Host", "trusted.internal");

        Map<String, String> config = Map.of(SKIP_OTP_FOR_HTTP_HEADER, "X-Forwarded-Host: trusted.internal");
        Object decision = invokeVoteForHttpHeader(authenticator, request, config);

        Assert.assertEquals("ABSTAIN", decision.toString());
    }

    @Test
    public void testSkipOtpIgnoredWhenProxyNotTrusted() throws Exception {
        ConditionalOtpFormAuthenticator authenticator = new ConditionalOtpFormAuthenticator();
        authenticator.setProxyConfigured(true);

        SimpleTestHttpRequest request = new SimpleTestHttpRequest(false); // untrusted proxy
        request.addHeader("X-Forwarded-Host", "trusted.internal");

        Map<String, String> config = Map.of(SKIP_OTP_FOR_HTTP_HEADER, "X-Forwarded-Host: trusted.internal");
        Object decision = invokeVoteForHttpHeader(authenticator, request, config);

        Assert.assertEquals("ABSTAIN", decision.toString());
    }

    @Test
    public void testSkipOtpHonoredWhenProxyConfiguredAndTrusted() throws Exception {
        ConditionalOtpFormAuthenticator authenticator = new ConditionalOtpFormAuthenticator();
        authenticator.setProxyConfigured(true);

        SimpleTestHttpRequest request = new SimpleTestHttpRequest(true); // trusted proxy
        request.addHeader("X-Forwarded-Host", "trusted.internal");

        Map<String, String> config = Map.of(SKIP_OTP_FOR_HTTP_HEADER, "X-Forwarded-Host: trusted.internal");
        Object decision = invokeVoteForHttpHeader(authenticator, request, config);

        Assert.assertEquals("SKIP_OTP", decision.toString());
    }

    @Test
    public void testForceOtpAlwaysEvaluated() throws Exception {
        ConditionalOtpFormAuthenticator authenticator = new ConditionalOtpFormAuthenticator();
        authenticator.setProxyConfigured(false);

        SimpleTestHttpRequest request = new SimpleTestHttpRequest(false);
        request.addHeader("Host", "localhost:8080");

        Map<String, String> config = Map.of(FORCE_OTP_FOR_HTTP_HEADER, "Host: localhost:8080");
        Object decision = invokeVoteForHttpHeader(authenticator, request, config);

        Assert.assertEquals("SHOW_OTP", decision.toString());
    }
}
