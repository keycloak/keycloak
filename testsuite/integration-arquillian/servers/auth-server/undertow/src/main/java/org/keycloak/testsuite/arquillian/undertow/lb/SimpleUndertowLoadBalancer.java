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

package org.keycloak.testsuite.arquillian.undertow.lb;

import java.lang.reflect.Field;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.ExclusivityChecker;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyCallback;
import io.undertow.server.handlers.proxy.ProxyClient;
import io.undertow.server.handlers.proxy.ProxyConnection;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import org.jboss.logging.Logger;
import org.keycloak.common.util.reflections.Reflections;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.testsuite.utils.tls.TLSUtils;

import io.undertow.server.handlers.proxy.RouteIteratorFactory;
import io.undertow.server.handlers.proxy.RouteIteratorFactory.ParsingCompatibility;
import io.undertow.server.handlers.proxy.RouteParsingStrategy;
import org.xnio.OptionMap;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;

/**
 * Loadbalancer on embedded undertow. Supports sticky session over "AUTH_SESSION_ID" cookie and failover to different node when sticky node not available.
 * Status 503 is returned just if all backend nodes are unavailable.
 *
 * To configure backend nodes, you can use system property like : -Dkeycloak.nodes="node1=http://localhost:8181,node2=http://localhost:8182"
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SimpleUndertowLoadBalancer {

    private static final Logger log = Logger.getLogger(SimpleUndertowLoadBalancer.class);

    static final String DEFAULT_NODES_HTTP = "node1=http://localhost:8181,node2=http://localhost:8182";

    private final String host;
    private final int httpPort;
    private final int httpsPort;
    private final Map<String, URI> backendNodes;
    private Undertow undertow;
    private LoadBalancingProxyClient lb;


    public static void main(String[] args) throws Exception {
        String nodes = System.getProperty("keycloak.nodes", DEFAULT_NODES_HTTP);

        SimpleUndertowLoadBalancer lb = new SimpleUndertowLoadBalancer("localhost", 8180, 8543, nodes);
        lb.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                lb.stop();
            }

        });
    }


    public SimpleUndertowLoadBalancer(String host, int httpPort, int httpsPort, String nodesString) {
        this.host = host;
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        this.backendNodes = parseNodes(nodesString);
        log.infof("Keycloak nodes: %s", backendNodes);
    }


    public void start() {
        try {
            HttpHandler proxyHandler = createHandler();

            undertow = Undertow.builder()
                    .addHttpListener(httpPort, host)
                    .addHttpsListener(httpsPort, host, TLSUtils.initializeTLS())
                    .setHandler(proxyHandler)
                    .build();
            undertow.start();

            backendNodes.forEach((route, uri) -> {
                lb.addHost(uri, route);
                log.debugf("Added host: %s, route: %s", uri.toString(), route);
            });

            log.infof("#### Loadbalancer started and ready to serve requests on http://%s:%d, https://%s:%d ####", host, httpPort, host, httpsPort);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void stop() {
        undertow.stop();
    }

    public void enableAllBackendNodes() {
        backendNodes.forEach((route, uri) -> {
            lb.removeHost(uri);
            lb.addHost(uri, route);
        });
        log.infof("Load balancer: enable all nodes. All enabled nodes: %s", lb.toString());
    }

    public void disableAllBackendNodes() {
        backendNodes.values().forEach(lb::removeHost);
        log.infof("Load balancer: disabling all nodes");
    }

    public void enableBackendNodeByName(String nodeName) {
        URI uri = backendNodes.get(nodeName);
        if (uri == null) {
            throw new IllegalArgumentException("Invalid node: " + nodeName);
        }
        lb.addHost(uri, nodeName);
        log.infof("Load balancer: enabled node '%s', All enabled nodes: %s", nodeName, lb.toString());
    }

    public void disableBackendNodeByName(String nodeName) {
        URI uri = backendNodes.get(nodeName);
        if (uri == null) {
            throw new IllegalArgumentException("Invalid node: " + nodeName);
        }
        lb.removeHost(uri);
        log.infof("Load balancer: disabled node '%s', All enabled nodes: %s", nodeName, lb.toString());
    }

    static Map<String, URI> parseNodes(String nodes) {
        StringTokenizer st = new StringTokenizer(nodes, ",");
        Map<String, URI> result = new LinkedHashMap<>();

        while (st.hasMoreElements()) {
            String nodeStr = st.nextToken();
            String[] node = nodeStr.trim().split("=", 2);
            result.put(node[0].trim(), URI.create(node[1].trim()));
        }

        return result;
    }


    private HttpHandler createHandler() throws Exception {

        // TODO: configurable options if needed
        String sessionCookieNames = AuthenticationSessionManager.AUTH_SESSION_ID;
        int connectionsPerThread = 20;
        int problemServerRetry = 5; // In case of unavailable node, we will try to ping him every 5 seconds to check if it's back
        int maxTime = 3600000; // 1 hour for proxy request timeout, so we can debug the backend keycloak servers
        int requestQueueSize = 10;
        int cachedConnectionsPerThread = 10;
        int connectionIdleTimeout = 60;
        int maxRetryAttempts = backendNodes.size() - 1;

        lb = new CustomLoadBalancingClient(exchange -> exchange.getRequestHeaders().contains(Headers.UPGRADE), maxRetryAttempts)
                .setConnectionsPerThread(connectionsPerThread)
                .setMaxQueueSize(requestQueueSize)
                .setSoftMaxConnectionsPerThread(cachedConnectionsPerThread)
                .setTtl(connectionIdleTimeout)
                .setProblemServerRetry(problemServerRetry);
        String[] sessionIds = sessionCookieNames.split(",");
        for (String id : sessionIds) {
            lb.addSessionCookieName(id);
        }

        return new ProxyHandler(lb, maxTime, ResponseCodeHandler.HANDLE_404);
    }


    private class CustomLoadBalancingClient extends LoadBalancingProxyClient {

        private final int maxRetryAttempts;

        public CustomLoadBalancingClient(ExclusivityChecker checker, int maxRetryAttempts) {
            super(checker);
            this.maxRetryAttempts = maxRetryAttempts;
        }


        @Override
        protected Host selectHost(HttpServerExchange exchange) {
            Host host = super.selectHost(exchange);

            if (host != null) {
                log.debugf("Selected host: %s, host available: %b", host.getUri().toString(), host.isAvailable());
            } else {
                log.warn("No host available");
            }

            exchange.putAttachment(SELECTED_HOST, host);
            return host;
        }

        private Host getRoute(String routeId) {
            // There's no way to get the route from the super class, we have to use reflection
            Field f = Reflections.findDeclaredField(LoadBalancingProxyClient.class, "routes");
            f.setAccessible(true);
            Map<String, Host> routes = Reflections.getFieldValue(f, this, Map.class);
            return routes == null ? null : routes.get(routeId);
        }

        @Override
        protected Iterator<CharSequence> parseRoutes(HttpServerExchange exchange) {
            Iterator<CharSequence> stickyHostsIt = super.parseRoutes(exchange);
            
            if (stickyHostsIt == null) {
                return null;
            }
            
            List<CharSequence> stickyHosts = new LinkedList<>();
            stickyHostsIt.forEachRemaining(stickyHosts::add);
            CharSequence stickyHostName = stickyHosts.isEmpty() ? null : stickyHosts.iterator().next();
            Host stickyHost = stickyHostName == null ? null: getRoute(stickyHostName.toString());
            if (stickyHost != null) {

                if (!stickyHost.isAvailable()) {
                    log.debugf("Sticky host %s not available. Trying different hosts", stickyHost.getUri());
                    return new RouteIteratorFactory(RouteParsingStrategy.SINGLE, ParsingCompatibility.MOD_JK, null).iterator(null);
                } else {
                    log.debugf("Sticky host %s found and looks available", stickyHost.getUri());
                }
            }

            return stickyHosts.iterator();
        }


        // For now, overriden just this "addHost" method to avoid adding duplicates
        @Override
        public synchronized LoadBalancingProxyClient addHost(URI host, String jvmRoute) {
            List<String> current = getCurrentHostRoutes();
            if (current.contains(jvmRoute)) {
                log.infof("Route '%s' already present. Skip adding", jvmRoute);
                return this;
            } else {
                try {
                    return super.addHost(host, jvmRoute, undertow.getXnio().getSslProvider(OptionMap.EMPTY));
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException(e);
                }
            }
        }


        @Override
        public void getConnection(ProxyTarget target, HttpServerExchange exchange, ProxyCallback<ProxyConnection> callback, long timeout, TimeUnit timeUnit) {
            long timeoutMs = timeUnit.toMillis(timeout);

            ProxyCallbackDelegate callbackDelegate = new ProxyCallbackDelegate(this, callback, timeoutMs, maxRetryAttempts);
            super.getConnection(target, exchange, callbackDelegate, timeout, timeUnit);
        }


        @Override
        public String toString() {
            return getCurrentHostRoutes().toString();
        }


        private List<String> getCurrentHostRoutes() {
            Field hostsField = Reflections.findDeclaredField(LoadBalancingProxyClient.class, "hosts");
            hostsField.setAccessible(true);
            Host[] hosts = (Host[]) Reflections.getFieldValue(hostsField, this);

            if (hosts == null) {
                return Collections.emptyList();
            }

            List<String> hostRoutes = new LinkedList<>();
            for (Host host : hosts) {
                Field hostField = Reflections.findDeclaredField(Host.class, "jvmRoute");
                hostField.setAccessible(true);
                String route = Reflections.getFieldValue(hostField, host).toString();
                hostRoutes.add(route);
            }
            return hostRoutes;
        }
    }


    private static final AttachmentKey<LoadBalancingProxyClient.Host> SELECTED_HOST = AttachmentKey.create(LoadBalancingProxyClient.Host.class);
    private static final AttachmentKey<Integer> REMAINING_RETRY_ATTEMPTS = AttachmentKey.create(Integer.class);


    private class ProxyCallbackDelegate implements ProxyCallback<ProxyConnection> {

        private final ProxyClient proxyClient;
        private final ProxyCallback<ProxyConnection> delegate;
        private final long timeoutMs;
        private final int maxRetryAttempts;


        public ProxyCallbackDelegate(ProxyClient proxyClient, ProxyCallback<ProxyConnection> delegate, long timeoutMs, int maxRetryAttempts) {
            this.proxyClient = proxyClient;
            this.delegate = delegate;
            this.timeoutMs = timeoutMs;
            this.maxRetryAttempts = maxRetryAttempts;
        }


        @Override
        public void completed(HttpServerExchange exchange, ProxyConnection result) {
            LoadBalancingProxyClient.Host host = exchange.getAttachment(SELECTED_HOST);
            if (host == null) {
                // shouldn't happen
                log.error("Host is null!!!");
            } else {
                // Host was restored
                if (!host.isAvailable()) {
                    log.infof("Host %s available again after failover", host.getUri());
                    host.clearError();
                }
            }

            delegate.completed(exchange, result);
        }


        @Override
        public void failed(HttpServerExchange exchange) {
            final long time = System.currentTimeMillis();

            Integer remainingAttempts = exchange.getAttachment(REMAINING_RETRY_ATTEMPTS);
            if (remainingAttempts == null) {
                remainingAttempts = maxRetryAttempts;
            } else {
                remainingAttempts--;
            }

            exchange.putAttachment(REMAINING_RETRY_ATTEMPTS, remainingAttempts);

            log.infof("Failed request to selected host. Remaining attempts: %d", remainingAttempts);
            if (remainingAttempts > 0) {
                if (timeoutMs > 0 && time > timeoutMs) {
                    delegate.failed(exchange);
                } else {
                    ProxyClient.ProxyTarget target = proxyClient.findTarget(exchange);
                    if (target != null) {
                        final long remaining = timeoutMs > 0 ? timeoutMs - time : -1;
                        proxyClient.getConnection(target, exchange, this, remaining, TimeUnit.MILLISECONDS);
                    } else {
                        couldNotResolveBackend(exchange); // The context was registered when we started, so return 503
                    }
                }
            } else {
                couldNotResolveBackend(exchange);
            }
        }


        @Override
        public void couldNotResolveBackend(HttpServerExchange exchange) {
            log.warnf("Could not resolve backend when request to: %s", exchange.getRequestURI());
            delegate.couldNotResolveBackend(exchange);
        }


        @Override
        public void queuedRequestFailed(HttpServerExchange exchange) {
            delegate.queuedRequestFailed(exchange);
        }

    }
}
