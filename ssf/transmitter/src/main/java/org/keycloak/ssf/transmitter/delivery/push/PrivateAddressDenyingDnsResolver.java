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
package org.keycloak.ssf.transmitter.delivery.push;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.keycloak.ssf.transmitter.support.SsfPushUrlValidator;

import org.apache.http.conn.DnsResolver;

/**
 * Connection-time half of the push SSRF gate. {@link SsfPushUrlValidator}
 * resolves the receiver's host when the stream is configured, but the host
 * is resolved again when an event is delivered, so a receiver controlling its
 * DNS could answer with a public address at configuration time and a private
 * one at delivery time. This resolver applies the same deny policy to the
 * addresses the HTTP client actually connects to.
 *
 * <p>Fails closed: an empty answer, or an answer where any address is
 * private, is rejected as a whole rather than filtered, so the connection
 * cannot fall through to a private address. The hostname itself is left
 * untouched, so TLS SNI and hostname verification still run against it.
 */
public class PrivateAddressDenyingDnsResolver implements DnsResolver {

    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        InetAddress[] addresses = doResolve(host);
        if (addresses == null || addresses.length == 0) {
            throw new UnknownHostException("SSF push target host " + host + " did not resolve to any address");
        }
        for (InetAddress address : addresses) {
            if (SsfPushUrlValidator.isPrivateOrLoopback(address)) {
                throw new PrivateAddressException(host, address);
            }
        }
        return addresses;
    }

    protected InetAddress[] doResolve(String host) throws UnknownHostException {
        return InetAddress.getAllByName(host);
    }

    /**
     * Raised instead of connecting when a push target resolves to a private
     * address. Extends {@link UnknownHostException} because that is the only
     * checked exception the {@link DnsResolver} contract allows; the HTTP
     * client then reports it as a transport failure for the delivery attempt.
     */
    public static class PrivateAddressException extends UnknownHostException {

        public PrivateAddressException(String host, InetAddress address) {
            super("SSF push target host " + host + " resolves to a non-routable, loopback, link-local,"
                    + " or private-network address (" + address.getHostAddress() + ")");
        }
    }
}
