/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.util.ldap;

import java.security.SecureRandom;
import java.util.List;
import javax.net.ssl.SSLContext;

import org.apache.directory.api.ldap.extras.extended.startTls.StartTlsResponse;
import org.apache.directory.api.ldap.extras.extended.startTls.StartTlsResponseImpl;
import org.apache.directory.api.ldap.model.message.ExtendedRequest;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.extended.StartTlsFilter;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.filter.ssl.SslFilter;
import org.jboss.logging.Logger;

/**
 * Replacement for the stock AM27 {@link StartTlsHandler} that updates the default
 * enabled protocols and adds diagnostics. The filter-chain logic is identical to AM27
 * (add {@link SslFilter} + {@link StartTlsFilter}, then write the response — the
 * {@code StartTlsFilter} bypasses encryption for the response).
 *
 * <p><b>Protocol defaults:</b> the stock AM27 handler defaults to {@code TLSv1, TLSv1.1,
 * TLSv1.2}. Modern JDKs (16+) disable TLSv1 and TLSv1.1, making TLSv1.2 the only
 * effective protocol. This handler defaults to {@code TLSv1.2, TLSv1.3} instead,
 * enabling TLSv1.3 whose single-flight handshake avoids timing-sensitive races observed
 * in the multi-flight TLSv1.2 handshake under MINA's {@link SslFilter}.
 *
 * <p>This handler initializes its own {@link SSLContext} because the parent's
 * {@code sslContext} field is private and inaccessible from a subclass.
 */
public class TLS13StartTlsHandler extends StartTlsHandler {

    private static final Logger log = Logger.getLogger(TLS13StartTlsHandler.class);

    private SSLContext sslCtx;
    private List<String> ciphers;
    private List<String> protocols;
    private boolean needClientAuth;
    private boolean wantClientAuth;

    @Override
    public void setLdapServer(LdapServer ldapServer) {
        super.setLdapServer(ldapServer);

        try {
            sslCtx = SSLContext.getInstance("TLS");
            sslCtx.init(
                    ldapServer.getKeyManagerFactory().getKeyManagers(),
                    ldapServer.getTrustManagers(),
                    new SecureRandom());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SSLContext for StartTLS handler", e);
        }

        for (Transport transport : ldapServer.getTransports()) {
            if (transport instanceof TcpTransport) {
                TcpTransport tcp = (TcpTransport) transport;
                ciphers = tcp.getCipherSuite();
                protocols = tcp.getEnabledProtocols();
                needClientAuth = tcp.isNeedClientAuth();
                wantClientAuth = tcp.isWantClientAuth();
                break;
            }
        }
    }

    @Override
    public void handleExtendedOperation(LdapSession session, ExtendedRequest req) throws Exception {
        log.debug("Handling StartTLS request.");

        IoFilterChain chain = session.getIoSession().getFilterChain();
        SslFilter sslFilter = (SslFilter) chain.get("sslFilter");

        if (sslFilter == null) {
            sslFilter = new SslFilter(sslCtx);

            if (ciphers != null && !ciphers.isEmpty()) {
                sslFilter.setEnabledCipherSuites(ciphers.toArray(new String[0]));
            }
            if (protocols != null && !protocols.isEmpty()) {
                sslFilter.setEnabledProtocols(protocols.toArray(new String[0]));
            } else {
                sslFilter.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
            }
            sslFilter.setNeedClientAuth(needClientAuth);
            sslFilter.setWantClientAuth(wantClientAuth);

            chain.addFirst("startTls", new StartTlsFilter());
            chain.addFirst("sslFilter", sslFilter);
            log.debug("SslFilter + StartTlsFilter added to chain.");
        }

        StartTlsResponse res = new StartTlsResponseImpl(req.getMessageId());
        res.getLdapResult().setResultCode(ResultCodeEnum.SUCCESS);
        res.setResponseName(EXTENSION_OID);

        session.getIoSession().write(res);
        log.debug("StartTLS response sent (via StartTlsFilter bypass).");
    }
}
