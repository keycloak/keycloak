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

import java.net.URI;
import java.util.HashMap;
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
import org.keycloak.services.managers.AuthenticationSessionManager;

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

    static final String DEFAULT_NODES = "node1=http://localhost:8181,node2=http://localhost:8182";

    private final String host;
    private final int port;
    private final String nodesString;
    private Undertow undertow;


    public static void main(String[] args) throws Exception {
        String nodes = System.getProperty("keycloak.nodes", DEFAULT_NODES);

        SimpleUndertowLoadBalancer lb = new SimpleUndertowLoadBalancer("localhost", 8180, nodes);
        lb.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                lb.stop();
            }

        });
    }


    public SimpleUndertowLoadBalancer(String host, int port, String nodesString) {
        this.host = host;
        this.port = port;
        this.nodesString = nodesString;
        log.infof("Keycloak nodes: %s", nodesString);
    }


    public void start() {
        Map<String, String> nodes = parseNodes(nodesString);
        try {
            HttpHandler proxyHandler = createHandler(nodes);

            undertow = Undertow.builder()
                    .addHttpListener(port, host)
                    .setHandler(proxyHandler)
                    .build();
            undertow.start();

            log.infof("Loadbalancer started and ready to serve requests on http://%s:%d", host, port);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void stop() {
        undertow.stop();
    }


    static Map<String, String> parseNodes(String nodes) {
        String[] nodesArray = nodes.split(",");
        Map<String, String> result = new HashMap<>();

        for (String nodeStr : nodesArray) {
            String[] node = nodeStr.trim().split("=");
            if (node.length != 2) {
                throw new IllegalArgumentException("Illegal node format in the configuration: " + nodeStr);
            }
            result.put(node[0].trim(), node[1].trim());
        }

        return result;
    }


    private HttpHandler createHandler(Map<String, String> backendNodes) throws Exception {

        // TODO: configurable options if needed
        String sessionCookieNames = AuthenticationSessionManager.AUTH_SESSION_ID;
        int connectionsPerThread = 20;
        int problemServerRetry = 5; // In case of unavailable node, we will try to ping him every 5 seconds to check if it's back
        int maxTime = 3600000; // 1 hour for proxy request timeout, so we can debug the backend keycloak servers
        int requestQueueSize = 10;
        int cachedConnectionsPerThread = 10;
        int connectionIdleTimeout = 60;
        int maxRetryAttempts = backendNodes.size() - 1;

        final LoadBalancingProxyClient lb = new CustomLoadBalancingClient(new ExclusivityChecker() {

            @Override
            public boolean isExclusivityRequired(HttpServerExchange exchange) {
                //we always create a new connection for upgrade requests
                return exchange.getRequestHeaders().contains(Headers.UPGRADE);
            }

        }, maxRetryAttempts)
                .setConnectionsPerThread(connectionsPerThread)
                .setMaxQueueSize(requestQueueSize)
                .setSoftMaxConnectionsPerThread(cachedConnectionsPerThread)
                .setTtl(connectionIdleTimeout)
                .setProblemServerRetry(problemServerRetry);
        String[] sessionIds = sessionCookieNames.split(",");
        for (String id : sessionIds) {
            lb.addSessionCookieName(id);
        }

        for (Map.Entry<String, String> node : backendNodes.entrySet()) {
            String route = node.getKey();
            URI uri = new URI(node.getValue());

            lb.addHost(uri, route);
            log.infof("Added host: %s, route: %s", uri.toString(), route);
        }

        ProxyHandler handler = new ProxyHandler(lb, maxTime, ResponseCodeHandler.HANDLE_404);
        return handler;
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
            log.debugf("Selected host: %s, host available: %b", host.getUri().toString(), host.isAvailable());
            exchange.putAttachment(SELECTED_HOST, host);
            return host;
        }


        @Override
        protected Host findStickyHost(HttpServerExchange exchange) {
            Host stickyHost = super.findStickyHost(exchange);

            if (stickyHost != null) {

                if (!stickyHost.isAvailable()) {
                    log.infof("Sticky host %s not available. Trying different hosts", stickyHost.getUri());
                    return null;
                } else {
                    log.infof("Sticky host %s found and looks available", stickyHost.getUri());
                }
            }

            return stickyHost;
        }


        @Override
        public void getConnection(ProxyTarget target, HttpServerExchange exchange, ProxyCallback<ProxyConnection> callback, long timeout, TimeUnit timeUnit) {
            long timeoutMs = timeUnit.toMillis(timeout);

            ProxyCallbackDelegate callbackDelegate = new ProxyCallbackDelegate(this, callback, timeoutMs, maxRetryAttempts);
            super.getConnection(target, exchange, callbackDelegate, timeout, timeUnit);
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
                    log.infof("Host %s available again", host.getUri());
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
            delegate.couldNotResolveBackend(exchange);
        }


        @Override
        public void queuedRequestFailed(HttpServerExchange exchange) {
            delegate.queuedRequestFailed(exchange);
        }

    }
}
