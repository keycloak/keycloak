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

package org.keycloak.email.aws.credentials;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * The address checks that decide whether a credential endpoint from the environment may be spoken to
 * over plain HTTP, with a bearer token attached.
 * <p>
 * They live in one place because they were written twice and the two copies disagreed: one validated
 * IPv4 octets and the other accepted {@code 127.999.999.999}, which is not an address at all but is a
 * legal DNS name — and a name is exactly what these checks exist to keep out.
 * <p>
 * Nothing here resolves a name. {@link InetAddress#getByName} parses a literal without touching DNS,
 * and every method refuses anything that is not one: a lookup that answers {@code 127.0.0.1} today
 * says nothing about where the next one will point.
 */
final class LocalAddress {

    private static final String IPV4_OCTET = "(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)";
    private static final Pattern IPV4_LITERAL = Pattern.compile(IPV4_OCTET + "(\\." + IPV4_OCTET + "){3}");

    private LocalAddress() {
    }

    /** Strips the brackets {@code URI#getHost} keeps around an IPv6 literal. */
    static String unbracket(String host) {
        return host.length() > 1 && host.startsWith("[") && host.endsWith("]")
                ? host.substring(1, host.length() - 1)
                : host;
    }

    /** Whether {@code host} is a literal address on the loopback interface. Names are refused. */
    static boolean isLoopback(String host) {
        InetAddress address = literal(host);
        return address != null && address.isLoopbackAddress();
    }

    /**
     * Whether {@code host} is the same address as {@code expected}, both read as literals.
     * <p>
     * Compared as addresses rather than as strings: {@code fd00:ec2::254} has a dozen legal spellings,
     * and a string comparison would accept only the one that happens to be written here — while a
     * prefix comparison, which is what this replaced, would accept the whole {@code fd00:ec2::/32}
     * range instead of the one endpoint AWS documents.
     */
    static boolean is(String host, String expected) {
        InetAddress address = literal(host);
        InetAddress target = literal(expected);
        return address != null && target != null && address.equals(target);
    }

    private static InetAddress literal(String host) {
        if (host == null) {
            return null;
        }
        String bare = unbracket(host);
        // A colon means IPv6, which getByName always parses rather than resolves; anything else has
        // to look exactly like a dotted quad before it is handed over.
        if (!bare.contains(":") && !IPV4_LITERAL.matcher(bare).matches()) {
            return null;
        }
        try {
            return InetAddress.getByName(bare);
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
