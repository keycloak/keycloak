package org.keycloak.proxy;

import io.undertow.Undertow;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.impl.CachedAuthenticatedSessionMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.server.handlers.proxy.SimpleProxyClientProvider;
import io.undertow.server.session.InMemorySessionManager;
import io.undertow.server.session.SessionAttachmentHandler;
import io.undertow.server.session.SessionCookieConfig;
import io.undertow.server.session.SessionManager;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.undertow.UndertowAuthenticatedActionsHandler;
import org.keycloak.adapters.undertow.UndertowAuthenticationMechanism;
import org.keycloak.adapters.undertow.UndertowPreAuthActionsHandler;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;
import org.keycloak.enums.SslRequired;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.xnio.Option;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProxyServerBuilder {
    public static final HttpHandler NOT_FOUND = new HttpHandler() {
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            exchange.setResponseCode(404);
            exchange.endExchange();
        }
    };

    protected Undertow.Builder builder = Undertow.builder();

    protected PathHandler root = new PathHandler(NOT_FOUND);
    protected HttpHandler proxyHandler;

    public ProxyServerBuilder target(String uri) {
        SimpleProxyClientProvider provider = null;
        try {
            provider = new SimpleProxyClientProvider(new URI(uri));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        final HttpHandler handler = new ProxyHandler(provider, 30000, ResponseCodeHandler.HANDLE_404);
        proxyHandler = new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                exchange.setRelativePath(exchange.getRequestPath()); // need this otherwise proxy forwards to chopped off path
                handler.handleRequest(exchange);
            }
        };
        return this;
    }

    public ApplicationBuilder application(AdapterConfig config) {
        return new ApplicationBuilder(config);
    }

    public class ApplicationBuilder {
        protected NodesRegistrationManagement nodesRegistrationManagement = new NodesRegistrationManagement();
        protected UndertowUserSessionManagement userSessionManagement = new UndertowUserSessionManagement();
        protected AdapterDeploymentContext deploymentContext;
        protected KeycloakDeployment deployment;
        SessionManager sessionManager = new InMemorySessionManager(
                "SESSION_MANAGER");
        protected String base;
        protected SecurityPathMatches.Builder constraintBuilder = new SecurityPathMatches.Builder();
        protected SecurityPathMatches matches;
        protected String errorPage;

        public ApplicationBuilder base(String base) {
            this.base = base;
            return this;
        }

        public ApplicationBuilder errorPage(String errorPage) {
            if (errorPage != null && errorPage.startsWith("/")) {
                errorPage = errorPage.substring(1);
            }
            this.errorPage = errorPage;
            return this;
        }

        public ApplicationBuilder(AdapterConfig config) {
            this.deployment = KeycloakDeploymentBuilder.build(config);
            this.deploymentContext = new AdapterDeploymentContext(deployment);
        }

        public ProxyServerBuilder add() {
            matches = constraintBuilder.build();
            HttpHandler handler = sessionHandling(addSecurity(proxyHandler));
            root.addPrefixPath(base, handler);
            return ProxyServerBuilder.this;
        }

        public ConstraintBuilder constraint(String pattern) {
            return new ConstraintBuilder(pattern);
        }

        public class ConstraintBuilder {
            protected String pattern;
            protected Set<String> rolesAllowed = new HashSet<String>();
            protected Set<String> methods = new HashSet<String>();
            protected Set<String> excludedMethods = new HashSet<String>();
            protected SecurityInfo.EmptyRoleSemantic semantic = SecurityInfo.EmptyRoleSemantic.AUTHENTICATE;

            public ConstraintBuilder(String pattern) {
                this.pattern = pattern;

            }

            public ConstraintBuilder deny() {
                semantic = SecurityInfo.EmptyRoleSemantic.DENY;
                return this;
            }
            public ConstraintBuilder permit() {
                semantic = SecurityInfo.EmptyRoleSemantic.PERMIT;
                return this;
            }
            public ConstraintBuilder authenticate() {
                semantic = SecurityInfo.EmptyRoleSemantic.AUTHENTICATE;
                return this;
            }

            public ConstraintBuilder method(String method) {
                methods.add(method);
                return this;
            }

            public ConstraintBuilder excludeMethod(String method) {
                excludedMethods.add(method);
                return this;
            }


            public ConstraintBuilder roles(String... roles) {
                for (String role : roles) role(role);
                return this;
            }

            public ConstraintBuilder role(String role) {
                rolesAllowed.add(role);
                return this;
            }

            public ApplicationBuilder add() {
                constraintBuilder.addSecurityConstraint(rolesAllowed, semantic, pattern, methods, excludedMethods);
                return ApplicationBuilder.this;
            }


        }

        private HttpHandler addSecurity(final HttpHandler toWrap) {
            HttpHandler handler = toWrap;
            handler = new UndertowAuthenticatedActionsHandler(deploymentContext, toWrap);
            if (errorPage != null) {
                if (base.endsWith("/")) {
                    errorPage = base + errorPage;
                } else {
                    errorPage = base + "/" + errorPage;
                }
            }
            handler = new ConstraintAuthorizationHandler(errorPage, handler);
            handler = new AuthenticationCallHandler(handler);
            handler = new ConstraintMatcherHandler(matches, handler, toWrap, errorPage);
            final List<AuthenticationMechanism> mechanisms = new LinkedList<AuthenticationMechanism>();
            mechanisms.add(new CachedAuthenticatedSessionMechanism());
            mechanisms.add(new UndertowAuthenticationMechanism(deploymentContext, userSessionManagement, nodesRegistrationManagement, -1));
            handler = new AuthenticationMechanismsHandler(handler, mechanisms);
            IdentityManager identityManager = new IdentityManager() {
                @Override
                public Account verify(Account account) {
                    return account;
                }

                @Override
                public Account verify(String id, Credential credential) {
                    throw new IllegalStateException("Should never be called in Keycloak flow");
                }

                @Override
                public Account verify(Credential credential) {
                    throw new IllegalStateException("Should never be called in Keycloak flow");
                }
            };
            handler = new UndertowPreAuthActionsHandler(deploymentContext, userSessionManagement, sessionManager, handler);
            return new SecurityInitialHandler(AuthenticationMode.PRO_ACTIVE, identityManager, handler);
        }

        private HttpHandler sessionHandling(HttpHandler toWrap) {
            SessionCookieConfig sessionConfig = new SessionCookieConfig();
            sessionConfig.setCookieName("keycloak." + deployment.getResourceName() + ".session");
            sessionConfig.setPath(base);
            if (deployment.getSslRequired() == SslRequired.ALL) sessionConfig.setSecure(true);
            toWrap = new SessionAttachmentHandler(
                    toWrap, sessionManager, sessionConfig);
            return toWrap;
        }

    }


    public Undertow build() {
        builder.setHandler(root);
        return builder.build();
    }

    public ProxyServerBuilder addHttpListener(int port, String host) {
        builder.addHttpListener(port, host);
        return this;
    }

    public ProxyServerBuilder addHttpsListener(int port, String host, KeyManager[] keyManagers, TrustManager[] trustManagers) {
        builder.addHttpsListener(port, host, keyManagers, trustManagers);
        return this;
    }

    public ProxyServerBuilder addHttpsListener(int port, String host, SSLContext sslContext) {
        builder.addHttpsListener(port, host, sslContext);
        return this;
    }

    public ProxyServerBuilder setBufferSize(int bufferSize) {
        builder.setBufferSize(bufferSize);
        return this;
    }

    public ProxyServerBuilder setBuffersPerRegion(int buffersPerRegion) {
        builder.setBuffersPerRegion(buffersPerRegion);
        return this;
    }

    public ProxyServerBuilder setIoThreads(int ioThreads) {
        builder.setIoThreads(ioThreads);
        return this;
    }

    public ProxyServerBuilder setWorkerThreads(int workerThreads) {
        builder.setWorkerThreads(workerThreads);
        return this;
    }

    public ProxyServerBuilder setDirectBuffers(boolean directBuffers) {
        builder.setDirectBuffers(directBuffers);
        return this;
    }

    public ProxyServerBuilder setHandler(HttpHandler handler) {
        builder.setHandler(handler);
        return this;
    }

    public <T> ProxyServerBuilder setServerOption(Option<T> option, T value) {
        builder.setServerOption(option, value);
        return this;
    }

    public <T> ProxyServerBuilder setSocketOption(Option<T> option, T value) {
        builder.setSocketOption(option, value);
        return this;
    }

    public <T> ProxyServerBuilder setWorkerOption(Option<T> option, T value) {
        builder.setWorkerOption(option, value);
        return this;
    }
}
