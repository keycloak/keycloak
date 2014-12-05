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
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.jboss.logging.Logger;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.FindFile;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.undertow.UndertowAuthenticatedActionsHandler;
import org.keycloak.adapters.undertow.UndertowAuthenticationMechanism;
import org.keycloak.adapters.undertow.UndertowPreAuthActionsHandler;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;
import org.keycloak.enums.SslRequired;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.util.CertificateUtils;
import org.keycloak.util.PemUtils;
import org.keycloak.util.SystemPropertiesJsonParserFactory;
import org.xnio.Option;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProxyServerBuilder {
    protected static Logger log = Logger.getLogger(ProxyServerBuilder.class);
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
    protected boolean sendAccessToken;

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

    public ProxyServerBuilder sendAccessToken(boolean flag) {
        this.sendAccessToken = flag;
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
            log.debugv("add constraint: {0}", pattern);
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

            public ConstraintBuilder excludedMethods(Set<String> excludedMethods) {
                this.excludedMethods = excludedMethods;
                return this;
            }

            public ConstraintBuilder methods(Set<String> methods) {
                this.methods = methods;
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
            public ConstraintBuilder roles(Set<String> roles) {
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
            handler = new ConstraintAuthorizationHandler(handler, errorPage, sendAccessToken);
            handler = new ProxyAuthenticationCallHandler(handler);
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

    public static ProxyConfig loadConfig(InputStream is) {
        ObjectMapper mapper = new ObjectMapper(new SystemPropertiesJsonParserFactory());
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_DEFAULT);
        ProxyConfig proxyConfig;
        try {
            proxyConfig = mapper.readValue(is, ProxyConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return proxyConfig;
    }
    public static Undertow build(InputStream configStream) {
        ProxyConfig config = loadConfig(configStream);
        return build(config);

    }

    public static Undertow build(ProxyConfig config) {
        ProxyServerBuilder builder = new ProxyServerBuilder();
        if (config.getTargetUrl() == null) {
            log.error("Must set Target URL");
            return null;
        }
        builder.target(config.getTargetUrl());
        if (config.getApplications() == null || config.getApplications().size() == 0) {
            log.error("No applications defined");
            return null;
        }
        initConnections(config, builder);
        initOptions(config, builder);

        for (ProxyConfig.Application application : config.getApplications()) {
            ApplicationBuilder applicationBuilder = builder.application(application.getAdapterConfig())
                    .base(application.getBasePath())
                    .errorPage(application.getErrorPage());

            if (application.getConstraints() != null) {
                for (ProxyConfig.Constraint constraint : application.getConstraints()) {
                    ApplicationBuilder.ConstraintBuilder constraintBuilder = applicationBuilder.constraint(constraint.getPattern());
                    if (constraint.getRolesAllowed() != null) {
                        constraintBuilder.roles(constraint.getRolesAllowed());
                    }
                    if (constraint.getMethods() != null) {
                        constraintBuilder.methods(constraint.getMethods());
                    }
                    if (constraint.getExcludedMethods() != null) {
                        constraintBuilder.excludedMethods(constraint.getExcludedMethods());
                    }
                    if (constraint.isDeny()) constraintBuilder.deny();
                    if (constraint.isPermit()) constraintBuilder.permit();
                    if (constraint.isAuthenticate()) constraintBuilder.authenticate();
                    constraintBuilder.add();
                }
            }
            applicationBuilder.add();
        }
        return builder.build();
    }

    public static void initOptions(ProxyConfig config, ProxyServerBuilder builder) {
        builder.sendAccessToken(config.isSendAccessToken());
        if (config.getBufferSize() != null) builder.setBufferSize(config.getBufferSize());
        if (config.getBuffersPerRegion() != null) builder.setBuffersPerRegion(config.getBuffersPerRegion());
        if (config.getIoThreads() != null) builder.setIoThreads(config.getIoThreads());
        if (config.getWorkerThreads() != null) builder.setWorkerThreads(config.getWorkerThreads());
        if (config.getDirectBuffers() != null) builder.setDirectBuffers(config.getDirectBuffers());
    }

    public static void initConnections(ProxyConfig config, ProxyServerBuilder builder) {
        if (config.getHttpPort() == null && config.getHttpsPort() == null) {
            log.warn("You have not set up HTTP or HTTPS");
        }
        if (config.getHttpPort() != null) {
            String bindAddress = "localhost";
            if (config.getBindAddress() != null) bindAddress = config.getBindAddress();
            builder.addHttpListener(config.getHttpPort(), bindAddress);
        }
        if (config.getHttpsPort() != null) {
            String bindAddress = "localhost";
            if (config.getBindAddress() != null) bindAddress = config.getBindAddress();
            if (config.getKeystore() != null) {
                InputStream is = FindFile.findFile(config.getKeystore());
                SSLContext sslContext = null;
                try {
                    KeyStore keystore = KeyStore.getInstance("jks");
                    keystore.load(is, config.getKeystorePassword().toCharArray());
                    sslContext = SslUtil.createSSLContext(keystore, config.getKeyPassword(), null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                builder.addHttpsListener(config.getHttpsPort().intValue(), bindAddress, sslContext);
            } else {
                log.warn("Generating temporary SSL cert");
                KeyPair keyPair = null;
                try {
                    keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                X509Certificate certificate = null;
                try {
                    certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, bindAddress);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                try {
                    KeyStore keyStore = KeyStore.getInstance("JKS");
                    keyStore.load(null, null);
                    PrivateKey privateKey = keyPair.getPrivate();


                    Certificate[] chain =  {certificate};

                    keyStore.setKeyEntry(bindAddress, privateKey, "password".toCharArray(), chain);
                    SSLContext sslContext = SslUtil.createSSLContext(keyStore, "password", null);
                    builder.addHttpsListener(config.getHttpsPort().intValue(), bindAddress, sslContext);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
