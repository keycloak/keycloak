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
