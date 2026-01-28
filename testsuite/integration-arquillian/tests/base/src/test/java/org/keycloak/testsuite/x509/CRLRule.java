/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.x509;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import io.undertow.Undertow;
import io.undertow.io.Sender;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;
import org.junit.rules.ExternalResource;

/**
 * Starts/stops embedded undertow server before/after the test class.
 * The server will serve some predefined CRL lists under URL like "http://localhost:8889/empty.crl"
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CRLRule extends ExternalResource {

    protected static final Logger log = Logger.getLogger(CRLRule.class);

    private static final String CRL_RESPONDER_HOST = "localhost";
    private static final int CRL_RESPONDER_PORT = 8889;

    public static final String CRL_RESPONDER_ORIGIN = "http://" + CRL_RESPONDER_HOST + ":" + CRL_RESPONDER_PORT;

    private Undertow crlResponder;
    private Map<String, CRLHandler> handlers;
    private PathHandler pathHandler;

    @Override
    protected void before() throws Throwable {
        log.info("Starting CRL Responder");

        handlers = new HashMap<>();
        pathHandler = new PathHandler();
        addHandler(AbstractX509AuthenticationTest.EMPTY_CRL_PATH, AbstractX509AuthenticationTest.EMPTY_CRL_PATH);
        addHandler(AbstractX509AuthenticationTest.EMPTY_EXPIRED_CRL_PATH, AbstractX509AuthenticationTest.EMPTY_EXPIRED_CRL_PATH);
        addHandler(AbstractX509AuthenticationTest.INTERMEDIATE_CA_CRL_PATH, AbstractX509AuthenticationTest.INTERMEDIATE_CA_CRL_PATH);
        addHandler(AbstractX509AuthenticationTest.INTERMEDIATE_CA_INVALID_SIGNATURE_CRL_PATH, AbstractX509AuthenticationTest.INTERMEDIATE_CA_INVALID_SIGNATURE_CRL_PATH);
        addHandler(AbstractX509AuthenticationTest.INTERMEDIATE_CA_3_CRL_PATH, AbstractX509AuthenticationTest.INTERMEDIATE_CA_3_CRL_PATH);
        addHandler(AbstractX509AuthenticationTest.INVALID_CRL_PATH, AbstractX509AuthenticationTest.INVALID_CRL_PATH);

        crlResponder = Undertow.builder().addHttpListener(CRL_RESPONDER_PORT, CRL_RESPONDER_HOST)
                .setHandler(
                        new BlockingHandler(pathHandler)
                ).build();

        crlResponder.start();
    }

    @Override
    protected void after() {
        log.info("Stoping CRL Responder");
        crlResponder.stop();
    }

    public void addHandler(String path, String crlFileName) {
        CRLHandler handler = new CRLHandler(crlFileName);
        handlers.put(path, handler);
        pathHandler.addExactPath(path, handler);
    }

    public void removeHandler(String path) {
        handlers.remove(path);
        pathHandler.removeExactPath(path);
    }

    public void setCrlForHandler(String path, String crlFileName) {
        handlers.get(path).SetFileName(crlFileName);
    }

    public int getCounter(String path) {
        return handlers.get(path).getCounter();
    }

    public void resetCounter(String path) {
        handlers.get(path).resetCounter();
    }

    private class CRLHandler implements HttpHandler {

        private String crlFileName;
        private int counter;

        public CRLHandler(String crlFileName) {
            this.crlFileName = crlFileName;
            counter = 0;
        }

        public void SetFileName(String crlFileName) {
            this.crlFileName = crlFileName;
        }

        public int getCounter() {
            return counter;
        }

        public void resetCounter() {
            counter = 0;
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            if (exchange.isInIoThread()) {
                exchange.dispatch(this);
                return;
            }

            counter++;
            String fullFile = AbstractX509AuthenticationTest.getAuthServerHome() + File.separator + crlFileName;
            InputStream is = new FileInputStream(new File(fullFile));

            final byte[] responseBytes = IOUtils.toByteArray(is);

            final HeaderMap responseHeaders = exchange.getResponseHeaders();
            responseHeaders.put(Headers.CONTENT_TYPE, "application/pkix-crl");
            // TODO: Add caching support? CRLs provided by well-known CA usually adds them

            final Sender responseSender = exchange.getResponseSender();
            responseSender.send(ByteBuffer.wrap(responseBytes));

            exchange.endExchange();
        }
    }
}
