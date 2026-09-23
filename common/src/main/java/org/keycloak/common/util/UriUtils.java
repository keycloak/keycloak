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
     */
    public static boolean hasExplicitPort(URI uri) {
        if (uri == null) {
            return false;
        }
        if (uri.getPort() != -1) {
            return true;
        }
        String hostPort = hostPortFromAuthority(uri.getRawAuthority());
        return hostPort != null && hostPort.indexOf(':') >= 0;
    }

    private static boolean hostsEqual(URI uriA, URI uriB) {
        String hostA = uriA.getHost();
        String hostB = uriB.getHost();
        if (hostA != null && hostB != null) {
            return hostA.equalsIgnoreCase(hostB);
        }
        if (hostA != null || hostB != null) {
            return false;
        }
        // Both hosts null: either opaque/no-authority, or a registry-name authority
        // (e.g. underscores) where Java leaves getHost() null.
        String authorityA = uriA.getRawAuthority();
        String authorityB = uriB.getRawAuthority();
        if (authorityA == null && authorityB == null) {
            // Opaque URIs (e.g. mailto:) have no authority; compare the scheme-specific part
            // so distinct targets are not treated as equal by scheme alone.
            return Objects.equals(uriA.getRawSchemeSpecificPart(), uriB.getRawSchemeSpecificPart());
        }
        if (authorityA == null || authorityB == null) {
            return false;
        }
        // Origin is scheme + host + port only; user-info is not compared here.
        String hostOnlyA = hostFromHostPort(hostPortFromAuthority(authorityA));
        String hostOnlyB = hostFromHostPort(hostPortFromAuthority(authorityB));
        return hostOnlyA.equalsIgnoreCase(hostOnlyB);
    }

    private static boolean portsEqual(URI uriA, URI uriB) {
        int portA = uriA.getPort();
        int portB = uriB.getPort();
        if (portA != -1 && portB != -1) {
            return portA == portB;
        }
        Integer explicitA = portA != -1 ? Integer.valueOf(portA) : explicitPortFromAuthority(uriA.getRawAuthority());
        Integer explicitB = portB != -1 ? Integer.valueOf(portB) : explicitPortFromAuthority(uriB.getRawAuthority());
        return Objects.equals(explicitA, explicitB);
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

    private static Integer explicitPortFromAuthority(String authority) {
        String hostPort = hostPortFromAuthority(authority);
        if (hostPort == null) {
            return null;
        }
        if (hostPort.startsWith("[")) {
            int close = hostPort.indexOf(']');
            if (close < 0 || close + 1 >= hostPort.length() || hostPort.charAt(close + 1) != ':') {
                return null;
            }
            String port = hostPort.substring(close + 2);
            if (port.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        int colon = hostPort.lastIndexOf(':');
        if (colon < 0 || colon == hostPort.length() - 1) {
            return null;
        }
        try {
            return Integer.parseInt(hostPort.substring(colon + 1));
        } catch (NumberFormatException e) {
            return null;
        }
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
