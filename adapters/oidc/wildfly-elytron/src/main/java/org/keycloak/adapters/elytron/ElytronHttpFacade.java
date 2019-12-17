/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters.elytron;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.adapters.spi.LogoutError;
import org.keycloak.enums.TokenStore;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.http.HttpScope;
import org.wildfly.security.http.HttpServerCookie;
import org.wildfly.security.http.HttpServerRequest;
import org.wildfly.security.http.HttpServerResponse;
import org.wildfly.security.http.Scope;

import javax.security.auth.callback.CallbackHandler;
import javax.security.cert.X509Certificate;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
class ElytronHttpFacade implements OIDCHttpFacade {

    static final String UNDERTOW_EXCHANGE = ElytronHttpFacade.class.getName() + ".undertow.exchange";

    private final HttpServerRequest request;
    private final CallbackHandler callbackHandler;
    private final AdapterTokenStore tokenStore;
    private final AdapterDeploymentContext deploymentContext;
    private Consumer<HttpServerResponse> responseConsumer;
    private ElytronAccount account;
    private SecurityIdentity securityIdentity;
    private boolean restored;
    private final Map<String, String> headers = new HashMap<>();

    public ElytronHttpFacade(HttpServerRequest request, AdapterDeploymentContext deploymentContext, CallbackHandler handler) {
        this.request = request;
        this.deploymentContext = deploymentContext;
        this.callbackHandler = handler;
        this.tokenStore = createTokenStore();
        this.responseConsumer = response -> {};
    }

    void authenticationComplete(ElytronAccount account, boolean storeToken) {
        this.securityIdentity = SecurityIdentityUtil.authorize(this.callbackHandler, account.getPrincipal());

        if (securityIdentity != null) {
            this.account = account;
            RefreshableKeycloakSecurityContext keycloakSecurityContext = account.getKeycloakSecurityContext();
            account.setCurrentRequestInfo(keycloakSecurityContext.getDeployment(), this.tokenStore);
            if (storeToken) {
                this.tokenStore.saveAccountInfo(account);
            }
        }
    }

    void authenticationComplete() {
        if (securityIdentity != null) {
            HttpScope requestScope = request.getScope(Scope.EXCHANGE);
            RefreshableKeycloakSecurityContext keycloakSecurityContext = account.getKeycloakSecurityContext();

            requestScope.setAttachment(KeycloakSecurityContext.class.getName(), keycloakSecurityContext);

            this.request.authenticationComplete(response -> {
                if (!restored) {
                    responseConsumer.accept(response);
                }
            }, () -> ((ElytronTokeStore) tokenStore).logout(true));
        }
    }

    void authenticationFailed() {
        this.request.authenticationFailed("Authentication Failed", response -> responseConsumer.accept(response));
    }

    void noAuthenticationInProgress() {
        this.request.noAuthenticationInProgress();
    }

    void noAuthenticationInProgress(AuthChallenge challenge) {
        if (challenge != null) {
            challenge.challenge(this);
        }
        this.request.noAuthenticationInProgress(response -> responseConsumer.accept(response));
    }

    void authenticationInProgress() {
        this.request.authenticationInProgress(response -> responseConsumer.accept(response));
    }

    HttpScope getScope(Scope scope) {
        return request.getScope(scope);
    }

    HttpScope getScope(Scope scope, String id) {
        return request.getScope(scope, id);
    }

    Collection<String> getScopeIds(Scope scope) {
        return request.getScopeIds(scope);
    }

    AdapterTokenStore getTokenStore() {
        return this.tokenStore;
    }

    KeycloakDeployment getDeployment() {
        return deploymentContext.resolveDeployment(this);
    }

    private AdapterTokenStore createTokenStore() {
        KeycloakDeployment deployment = getDeployment();

        if (TokenStore.SESSION.equals(deployment.getTokenStore())) {
            return new ElytronSessionTokenStore(this, this.callbackHandler);
        } else {
            return new ElytronCookieTokenStore(this, this.callbackHandler);
        }
    }

    @Override
    public Request getRequest() {
        return new Request() {
            private InputStream inputStream;

            @Override
            public String getMethod() {
                return request.getRequestMethod();
            }

            @Override
            public String getURI() {
                try {
                    return URLDecoder.decode(request.getRequestURI().toString(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("Failed to decode request URI", e);
                }
            }

            @Override
            public String getRelativePath() {
                return request.getRequestPath();
            }

            @Override
            public boolean isSecure() {
                return request.getRequestURI().getScheme().equals("https");
            }

            @Override
            public String getFirstParam(String param) {
                return request.getFirstParameterValue(param);
            }

            @Override
            public String getQueryParamValue(String param) {
                URI requestURI = request.getRequestURI();
                String query = requestURI.getQuery();
                if (query != null) {
                    String[] parameters = query.split("&");
                    for (String parameter : parameters) {
                        String[] keyValue = parameter.split("=", 2);
                        if (keyValue[0].equals(param)) {
                            try {
                                return URLDecoder.decode(keyValue[1], "UTF-8");
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to decode request URI", e);
                            }
                        }
                    }
                }
                return null;
            }

            @Override
            public Cookie getCookie(final String cookieName) {
                List<HttpServerCookie> cookies = request.getCookies();

                if (cookies != null) {
                    for (HttpServerCookie cookie : cookies) {
                        if (cookie.getName().equals(cookieName)) {
                            return new Cookie(cookie.getName(), cookie.getValue(), cookie.getVersion(), cookie.getDomain(), cookie.getPath());
                        }
                    }
                }

                return null;
            }

            @Override
            public String getHeader(String name) {
                return request.getFirstRequestHeaderValue(name);
            }

            @Override
            public List<String> getHeaders(String name) {
                return request.getRequestHeaderValues(name);
            }

            @Override
            public InputStream getInputStream() {
                return getInputStream(false);
            }

            @Override
            public InputStream getInputStream(boolean buffered) {
                if (inputStream != null) {
                    return inputStream;
                }

                if (buffered) {
                    HttpScope exchangeScope = getScope(Scope.EXCHANGE);
                    HttpServerExchange exchange = ProtectedHttpServerExchange.class.cast(exchangeScope.getAttachment(UNDERTOW_EXCHANGE)).getExchange();
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

                return request.getInputStream();
            }

            @Override
            public String getRemoteAddr() {
                InetSocketAddress sourceAddress = request.getSourceAddress();
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
                request.getScope(Scope.EXCHANGE).setAttachment(AuthenticationError.class.getName(), error);
            }

            @Override
            public void setError(LogoutError error) {
                request.getScope(Scope.EXCHANGE).setAttachment(LogoutError.class.getName(), error);
            }
        };
    }

    @Override
    public Response getResponse() {
        return new Response() {

            @Override
            public void setStatus(final int status) {
                if (status < 200 || status > 300) {
                    responseConsumer = responseConsumer.andThen(response -> response.setStatusCode(status));
                }
            }

            @Override
            public void addHeader(final String name, final String value) {
                headers.put(name, value);
                responseConsumer = responseConsumer.andThen(new Consumer<HttpServerResponse>() {
                    @Override
                    public void accept(HttpServerResponse response) {
                        String latestValue = headers.get(name);

                        if (latestValue.equals(value)) {
                            response.addResponseHeader(name, latestValue);
                        }
                    }
                });
            }

            @Override
            public void setHeader(String name, String value) {
                addHeader(name, value);
            }

            @Override
            public void resetCookie(final String name, final String path) {
                responseConsumer = responseConsumer.andThen(response -> setCookie(name, "", path, null, 0, false, false, response));
                HttpScope exchangeScope = getScope(Scope.EXCHANGE);
                ProtectedHttpServerExchange undertowExchange = ProtectedHttpServerExchange.class.cast(exchangeScope.getAttachment(UNDERTOW_EXCHANGE));

                if (undertowExchange != null) {
                    CookieImpl cookie = new CookieImpl(name, "");

                    cookie.setMaxAge(0);
                    cookie.setPath(path);

                    undertowExchange.getExchange().setResponseCookie(cookie);
                }
            }

            @Override
            public void setCookie(final String name, final String value, final String path, final String domain, final int maxAge, final boolean secure, final boolean httpOnly) {
                responseConsumer = responseConsumer.andThen(response -> setCookie(name, value, path, domain, maxAge, secure, httpOnly, response));
            }

            private void setCookie(final String name, final String value, final String path, final String domain, final int maxAge, final boolean secure, final boolean httpOnly, HttpServerResponse response) {
                response.setResponseCookie(new HttpServerCookie() {
                    @Override
                    public String getName() {
                        return name;
                    }

                    @Override
                    public String getValue() {
                        return value;
                    }

                    @Override
                    public String getDomain() {
                        return domain;
                    }

                    @Override
                    public int getMaxAge() {
                        return maxAge;
                    }

                    @Override
                    public String getPath() {
                        return path;
                    }

                    @Override
                    public boolean isSecure() {
                        return secure;
                    }

                    @Override
                    public int getVersion() {
                        return 0;
                    }

                    @Override
                    public boolean isHttpOnly() {
                        return httpOnly;
                    }
                });
            }

            @Override
            public OutputStream getOutputStream() {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                responseConsumer = responseConsumer.andThen(new Consumer<HttpServerResponse>() {
                    @Override
                    public void accept(HttpServerResponse httpServerResponse) {
                        try {
                            httpServerResponse.getOutputStream().write(stream.toByteArray());
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to write to response output stream", e);
                        }
                    }
                });
                return stream;
            }

            @Override
            public void sendError(int code) {
                setStatus(code);
            }

            @Override
            public void sendError(final int code, final String message) {
                responseConsumer = responseConsumer.andThen(response -> {
                    response.setStatusCode(code);
                    response.addResponseHeader("Content-Type", "text/html");
                    try {
                        response.getOutputStream().write(message.getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            @Override
            public void end() {

            }
        };
    }

    @Override
    public X509Certificate[] getCertificateChain() {
        return new X509Certificate[0];
    }

    @Override
    public KeycloakSecurityContext getSecurityContext() {
        if (account == null) {
            return null;
        }
        return this.account.getKeycloakSecurityContext();
    }

    public boolean restoreRequest() {
        restored = this.request.resumeRequest();
        return restored;
    }

    public void suspendRequest() {
        responseConsumer = responseConsumer.andThen(httpServerResponse -> request.suspendRequest());
    }

    public boolean isAuthorized() {
        return this.securityIdentity != null;
    }
}
