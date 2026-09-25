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

package org.keycloak.common.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Objects;
import java.util.regex.Pattern;

import org.keycloak.common.enums.SslRequired;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UriUtils {

    private static final Pattern originPattern = Pattern.compile("(http://|https://)[\\w-]+(\\.[\\w-]+)*(:[\\d]{2,5})?");

    public static String getOrigin(URI uri) {
        return getOrigin(uri.toString());
    }

    public static String getOrigin(String uri) {
        String u = uri.toString();
        int e = u.indexOf('/', 8);
        return e != -1 ? u.substring(0, u.indexOf('/', 8)) : u;
    }

    public static boolean isOrigin(String url) {
        return originPattern.matcher(url).matches();
    }

    public static boolean originEquals(String originA, String originB) {
        if (Objects.equals(originA, originB)) {
            return true;
        }
        if (originA == null || originB == null) {
            return false;
        }
        try {
            return schemeHostAndPortEqual(new URI(originA), new URI(originB));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static boolean schemeAndHostEqual(URI uriA, URI uriB) {
        if (uriA == null || uriB == null) {
            return uriA == uriB;
        }
        String schemeA = uriA.getScheme();
        String schemeB = uriB.getScheme();
        if (schemeA == null || schemeB == null || !schemeA.equalsIgnoreCase(schemeB)) {
            return false;
        }
        return hostsEqual(uriA, uriB);
    }

    public static boolean schemeHostAndPortEqual(URI uriA, URI uriB) {
        if (uriA == null || uriB == null) {
            return uriA == uriB;
        }
        if (!schemeAndHostEqual(uriA, uriB)) {
            return false;
        }
        return portsEqual(uriA, uriB);
    }

    /**
     * Compare user-info case-sensitively, including registry-name authorities where
     * {@link URI#getRawUserInfo()} is left null by Java.
     */
    public static boolean rawUserInfoEqual(URI uriA, URI uriB) {
        if (uriA == null || uriB == null) {
            return uriA == uriB;
        }
        String userInfoA = uriA.getRawUserInfo();
        String userInfoB = uriB.getRawUserInfo();
        if (userInfoA != null || userInfoB != null) {
            return Objects.equals(userInfoA, userInfoB);
        }
        return Objects.equals(userInfoFromAuthority(uriA.getRawAuthority()),
                userInfoFromAuthority(uriB.getRawAuthority()));
    }

    /**
     * Whether the URI authority includes an explicit port separator (including an empty port).
     * Used for port-wildcard redirect prefixes such as {@code https://example.com:*}.
     * IPv6 literals are handled so colons inside {@code [...]} are not treated as a port.
     */
    public static boolean hasExplicitPort(URI uri) {
        if (uri == null) {
            return false;
        }
        if (uri.getPort() != -1) {
            return true;
        }
        return portSuffixFromAuthority(uri.getRawAuthority()) != null;
    }

    private static boolean hostsEqual(URI uriA, URI uriB) {
        String hostA = resolveHost(uriA);
        String hostB = resolveHost(uriB);
        if (hostA != null && hostB != null) {
            // ASCII-only: String.equalsIgnoreCase also folds Unicode (e.g. ı ↔ i).
            return asciiEqualsIgnoreCase(hostA, hostB);
        }
        if (hostA != null || hostB != null) {
            return false;
        }
        // Neither URI has an authority (opaque URIs such as mailto:). Compare the
        // scheme-specific part so distinct targets are not equal by scheme alone.
        if (uriA.getRawAuthority() == null && uriB.getRawAuthority() == null) {
            return Objects.equals(uriA.getRawSchemeSpecificPart(), uriB.getRawSchemeSpecificPart());
        }
        return false;
    }

    /**
     * Case-insensitive equality restricted to ASCII {@code A-Z}/{@code a-z}.
     * Unlike {@link String#equalsIgnoreCase(String)}, this does not fold Unicode
     * letters (e.g. Latin small letter dotless i {@code ı} must not match {@code i}).
     */
    private static boolean asciiEqualsIgnoreCase(String a, String b) {
        int len = a.length();
        if (len != b.length()) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            char ca = a.charAt(i);
            char cb = b.charAt(i);
            if (ca == cb) {
                continue;
            }
            if (ca >= 'A' && ca <= 'Z') {
                ca += 'a' - 'A';
            }
            if (cb >= 'A' && cb <= 'Z') {
                cb += 'a' - 'A';
            }
            if (ca != cb) {
                return false;
            }
        }
        return true;
    }

    /**
     * Host for comparison: {@link URI#getHost()} when present, otherwise the host
     * portion of the raw authority. Java leaves {@code getHost()} null for some
     * registry-names (e.g. underscores) and Unicode labels; those still appear in
     * {@link URI#getRawAuthority()}. The raw form is split on literal delimiters
     * first so percent-encoded {@code @} / {@code :} are not reinterpreted as
     * user-info or port separators (e.g. {@code evil%40good.com} stays one host).
     */
    private static String resolveHost(URI uri) {
        String host = uri.getHost();
        if (host != null) {
            return host;
        }
        String authority = uri.getRawAuthority();
        if (authority == null) {
            return null;
        }
        return hostFromHostPort(hostPortFromAuthority(authority));
    }

    private static boolean portsEqual(URI uriA, URI uriB) {
        int portA = uriA.getPort();
        int portB = uriB.getPort();
        if (portA != -1 && portB != -1) {
            return portA == portB;
        }
        // Compare raw port suffixes so absent ports (null) differ from empty (""), and
        // nonnumeric registry-name ports like "bar" vs "baz" are not collapsed to null.
        String suffixA = portA != -1 ? Integer.toString(portA) : portSuffixFromAuthority(uriA.getRawAuthority());
        String suffixB = portB != -1 ? Integer.toString(portB) : portSuffixFromAuthority(uriB.getRawAuthority());
        return Objects.equals(suffixA, suffixB);
    }

    private static String userInfoFromAuthority(String authority) {
        if (authority == null) {
            return null;
        }
        int at = authority.lastIndexOf('@');
        return at >= 0 ? authority.substring(0, at) : null;
    }

    private static String hostPortFromAuthority(String authority) {
        if (authority == null) {
            return null;
        }
        int at = authority.lastIndexOf('@');
        return at >= 0 ? authority.substring(at + 1) : authority;
    }

    private static String hostFromHostPort(String hostPort) {
        if (hostPort == null) {
            return "";
        }
        if (hostPort.startsWith("[")) {
            int close = hostPort.indexOf(']');
            return close >= 0 ? hostPort.substring(0, close + 1) : hostPort;
        }
        int colon = hostPort.lastIndexOf(':');
        return colon >= 0 ? hostPort.substring(0, colon) : hostPort;
    }

    /**
     * Port suffix from the authority, or {@code null} if there is no port separator.
     * An empty string means an explicit empty port (authority ends with {@code :}).
     * For IPv6, only a colon after the closing {@code ]} counts as a port separator.
     */
    private static String portSuffixFromAuthority(String authority) {
        String hostPort = hostPortFromAuthority(authority);
        if (hostPort == null) {
            return null;
        }
        if (hostPort.startsWith("[")) {
            int close = hostPort.indexOf(']');
            if (close < 0 || close + 1 >= hostPort.length() || hostPort.charAt(close + 1) != ':') {
                return null;
            }
            return hostPort.substring(close + 2);
        }
        int colon = hostPort.lastIndexOf(':');
        if (colon < 0) {
            return null;
        }
        return hostPort.substring(colon + 1);
    }

    public static String getHost(String uri) {
        try {
            if (uri == null) return null;
            URI url = new URI(uri);
            return url.getHost();
        } catch (URISyntaxException uriSyntaxException) {
            throw new IllegalArgumentException("URI '" + uri + "' is not valid.");
        }
    }

    public static MultivaluedHashMap<String, String> parseQueryParameters(String queryString, boolean decode) {
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<String, String>();
        if (queryString == null || queryString.equals("")) return map;

        String[] params = queryString.split("&");

        for (String param : params)
        {
            if (param.indexOf('=') >= 0)
            {
                String[] nv = param.split("=", 2);
                try
                {
                    String name = decode ? URLDecoder.decode(nv[0], "UTF-8") : nv[0];
                    String val = nv.length > 1 ? nv[1] : "";
                    map.add(name, decode ? URLDecoder.decode(val, "UTF-8") : val);
                }
                catch (UnsupportedEncodingException e)
                {
                    throw new RuntimeException(e);
                }
            }
            else
            {
                try
                {
                    String name = decode ? URLDecoder.decode(param, "UTF-8") : param;
                    map.add(name, "");
                }
                catch (UnsupportedEncodingException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
        return map;
    }

    public static MultivaluedHashMap<String, String> decodeQueryString(String queryString) {
        return parseQueryParameters(queryString, true);
    }

    public static String stripQueryParam(String url, String name){
        return url.replaceFirst("[\\?&]"+name+"=[^&]*$|"+name+"=[^&]*&", "");
    }

    public static void checkUrl(SslRequired sslRequired, String url, String name) throws IllegalArgumentException{
        if (url == null) {
            return;
        }

        URL parsed;

        try {
            parsed = new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("The url [" + name + "] is malformed", e);
        }

        String protocol = parsed.getProtocol().toLowerCase();

        if (!("http".equals(protocol) || "https".equals(protocol))) {
            throw new IllegalArgumentException("Invalid protocol/scheme for url [" + name + "]");
        }

        if (!"https".equals(protocol) && sslRequired.isRequired(parsed.getHost())) {
            throw new IllegalArgumentException("The url [" + name + "] requires secure connections");
        }
    }
}
