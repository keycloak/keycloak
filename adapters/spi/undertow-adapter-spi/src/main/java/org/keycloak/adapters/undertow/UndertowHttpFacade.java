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

package org.keycloak.adapters.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.LogoutError;
import org.keycloak.common.util.KeycloakUriBuilder;

import javax.security.cert.X509Certificate;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UndertowHttpFacade implements HttpFacade {
    public static final AttachmentKey<AuthenticationError> AUTH_ERROR_ATTACHMENT_KEY = AttachmentKey.create(AuthenticationError.class);
    public static final AttachmentKey<LogoutError> LOGOUT_ERROR_ATTACHMENT_KEY = AttachmentKey.create(LogoutError.class);

    protected HttpServerExchange exchange;
    protected RequestFacade requestFacade = new RequestFacade();
    protected ResponseFacade responseFacade = new ResponseFacade();

    public UndertowHttpFacade(HttpServerExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public Request getRequest() {
        return requestFacade;
    }

    @Override
    public Response getResponse() {
        return responseFacade;
    }

    @Override
    public X509Certificate[] getCertificateChain() {
        X509Certificate[] chain = new X509Certificate[0];
        try {
            chain = exchange.getConnection().getSslSessionInfo().getPeerCertificateChain();
        } catch (Exception ignore) {

        }
        return chain;
    }

    protected class RequestFacade implements Request {

        private InputStream inputStream;
        private final FormParserFactory formParserFactory = FormParserFactory.builder().build();
        private FormData formData;

        @Override
        public String getURI() {
            KeycloakUriBuilder uriBuilder = KeycloakUriBuilder.fromUri(exchange.getRequestURI())
                    .replaceQuery(exchange.getQueryString());
            if (!exchange.isHostIncludedInRequestURI()) uriBuilder.scheme(exchange.getRequestScheme()).host(exchange.getHostAndPort());
            return uriBuilder.buildAsString();
        }

        @Override
        public String getRelativePath() {
            return exchange.getRelativePath();
        }

        @Override
        public boolean isSecure() {
            String protocol = exchange.getRequestScheme();
            return protocol.equalsIgnoreCase("https");
        }

        @Override
        public String getFirstParam(String param) {
            Deque<String> values = exchange.getQueryParameters().get(param);

            if (values != null && !values.isEmpty()) {
                return values.getFirst();
            }

            if (formData == null && "post".equalsIgnoreCase(getMethod())) {
                FormDataParser parser = formParserFactory.createParser(exchange);
                try {
                    formData = parser.parseBlocking();
                } catch (IOException cause) {
                    throw new RuntimeException("Failed to parse form parameters", cause);
                }
            }

            if (formData != null) {
                Deque<FormValue> formValues = formData.get(param);

                if (formValues != null && !formValues.isEmpty()) {
                    FormValue firstValue = formValues.getFirst();

                    if (!firstValue.isFile()) {
                        return firstValue.getValue();
                    }
                }
            }

            return null;
        }

        @Override
        public String getQueryParamValue(String param) {
            Map<String,Deque<String>> queryParameters = exchange.getQueryParameters();
            if (queryParameters == null) return null;
            Deque<String> strings = queryParameters.get(param);
            if (strings == null) return null;
            return strings.getFirst();
        }

        @Override
        public Cookie getCookie(String cookieName) {
            Map<String, io.undertow.server.handlers.Cookie> requestCookies = exchange.getRequestCookies();
            if (requestCookies == null) return null;
            io.undertow.server.handlers.Cookie cookie = requestCookies.get(cookieName);
            if (cookie == null) return null;
            return new Cookie(cookie.getName(), cookie.getValue(), cookie.getVersion(), cookie.getDomain(), cookie.getPath());
        }

        @Override
        public List<String> getHeaders(String name) {
            return exchange.getRequestHeaders().get(name);
        }

        @Override
        public String getMethod() {
            return exchange.getRequestMethod().toString();
        }



        @Override
        public String getHeader(String name) {
            return exchange.getRequestHeaders().getFirst(name);
        }

        @Override
        public InputStream getInputStream() {
            return getInputStream(false);
        }

        @Override
        public InputStream getInputStream(boolean buffered) {
            if (!exchange.isBlocking()) exchange.startBlocking();

            if (inputStream != null) {
                return inputStream;
            }

            if (buffered) {
                ServletRequestContext context = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
                ServletRequest servletRequest = context.getServletRequest();

                inputStream = new BufferedInputStream(exchange.getInputStream());
                
                context.setServletRequest(new HttpServletRequestWrapper((HttpServletRequest) servletRequest) {
                    @Override
                    public ServletInputStream getInputStream() {
                        inputStream.mark(0);
                        return new ServletInputStream() {
                            @Override
                            public int read() throws IOException {
                                return inputStream.read();
                            }
                        };
                    }
                });
                return inputStream;
            }

            return exchange.getInputStream();
        }

        @Override
        public String getRemoteAddr() {
            InetSocketAddress sourceAddress = exchange.getSourceAddress();
            if (sourceAddress == null) {
                return "";
            }
            InetAddress address = sourceAddress.getAddress();
            if (address == null) {
                // this is unresolved, so we just return the host name not exactly spec, but if the name should be
                // resolved then a PeerNameResolvingHandler should be used and this is probably better than just
                // returning null
                return sourceAddress.getHostString();
            }
            return address.getHostAddress();
        }

        @Override
        public void setError(AuthenticationError error) {
            exchange.putAttachment(AUTH_ERROR_ATTACHMENT_KEY, error);
        }

        @Override
        public void setError(LogoutError error) {
            exchange.putAttachment(LOGOUT_ERROR_ATTACHMENT_KEY, error);

        }
    }

    protected class ResponseFacade implements Response {
        @Override
        public void setStatus(int status) {
            exchange.setResponseCode(status);
        }

        @Override
        public void addHeader(String name, String value) {
            exchange.getResponseHeaders().add(new HttpString(name), value);
        }

        @Override
        public void setHeader(String name, String value) {
            exchange.getResponseHeaders().put(new HttpString(name), value);
        }

        @Override
        public void resetCookie(String name, String path) {
            CookieImpl cookie = new CookieImpl(name, "");
            cookie.setMaxAge(0);
            cookie.setPath(path);
            exchange.setResponseCookie(cookie);
        }

        @Override
        public void setCookie(String name, String value, String path, String domain, int maxAge, boolean secure, boolean httpOnly) {
            CookieImpl cookie = new CookieImpl(name, value);
            cookie.setPath(path);
            cookie.setDomain(domain);
            cookie.setMaxAge(maxAge);
            cookie.setSecure(secure);
            cookie.setHttpOnly(httpOnly);
            exchange.setResponseCookie(cookie);
        }

        @Override
        public OutputStream getOutputStream() {
            if (!exchange.isBlocking()) exchange.startBlocking();
            return exchange.getOutputStream();
        }

        @Override
        public void sendError(int code) {
            exchange.setResponseCode(code);
        }

        @Override
        public void sendError(int code, String message) {
            exchange.setResponseCode(code);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
            try {
                exchange.getOutputStream().write(message.getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            exchange.endExchange();
        }


        @Override
        public void end() {
            exchange.endExchange();
        }
    }
}
