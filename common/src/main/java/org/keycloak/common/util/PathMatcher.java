/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class PathMatcher<P> {

    private static final char WILDCARD = '*';

    public P matches(final String targetUri) {
        final String normalizedUri = normalizeUri(targetUri);
        if (normalizedUri == null) {
            return null;
        }
        int patternCount = 0;
        int bracketsPatternCount = 0;
        P matchingPath = null;
        P matchingAnyPath = null;
        P matchingAnySuffixPath = null;

        for (P entry : getPaths()) {
            String expectedUri = getPath(entry);

            if (expectedUri == null || expectedUri.isEmpty()) {
                continue;
            }

            String matchingUri = null;

            if (exactMatch(expectedUri, normalizedUri)) {
                matchingUri = expectedUri;
            }

            if (isTemplate(expectedUri)) {
                String templateUri = buildUriFromTemplate(expectedUri, normalizedUri, false);

                if (templateUri != null) {
                    int length = expectedUri.split("\\/").length;
                    int bracketsLength = expectedUri.split("\\{").length;

                    if (exactMatch(templateUri, normalizedUri) && (patternCount == 0 || length > patternCount || bracketsLength < bracketsPatternCount)) {
                        matchingUri = templateUri;
                        P resolved = resolvePathConfig(entry, normalizedUri);

                        if (resolved != null) {
                            entry = resolved;
                        }

                        patternCount = length;
                        bracketsPatternCount = bracketsLength;
                    }
                }
            }

            if (matchingUri != null) {
                StringBuilder path = new StringBuilder(expectedUri);
                int patternIndex = path.indexOf("/" + WILDCARD);

                if (patternIndex != -1) {
                    path.delete(patternIndex, path.length());
                }

                patternIndex = path.indexOf("{");

                if (patternIndex != -1) {
                    path.delete(patternIndex, path.length());
                }

                String pathString = path.toString();

                if ("".equals(pathString)) {
                    pathString = "/";
                }

                if (matchingUri.equals(normalizedUri) || pathString.equals(normalizedUri)) {
                    if (patternCount == 0) {
                        return entry;
                    } else {
                        matchingPath = entry;
                    }
                }

                if (WILDCARD == expectedUri.charAt(expectedUri.length() - 1)) {
                    if (matchingAnyPath == null) {
                        matchingAnyPath = entry;
                    } else {
                        String resourcePath = getPath(matchingAnyPath);

                        if (resourcePath.split("/").length < matchingUri.split("/").length) {
                            matchingAnyPath = entry;
                        }
                    }
                } else {
                    int suffixIndex = expectedUri.indexOf(WILDCARD + ".");

                    if (suffixIndex != -1) {
                        String protectedSuffix = expectedUri.substring(suffixIndex + 1);

                        if (normalizedUri.endsWith(protectedSuffix)) {
                            matchingAnySuffixPath = entry;
                        }
                    }
                }
            }
        }

        if (matchingPath != null) {
            return matchingPath;
        }

        if (matchingAnySuffixPath != null) {
            return matchingAnySuffixPath;
        }

        return matchingAnyPath;
    }

    protected abstract String getPath(P entry);

    protected abstract Collection<P> getPaths();

    private boolean exactMatch(String expectedUri, String targetUri) {
        if (targetUri.equals(expectedUri)) {
            return true;
        }

        if (endsWithWildcard(expectedUri)) {
            String rootPath = expectedUri.substring(0, expectedUri.length() - 1);

            if (targetUri.startsWith(rootPath)) {
                return true;
            }

            return targetUri.equals(rootPath.substring(0, rootPath.length() - 1));
        }

        String suffix = "/*.";
        int suffixIndex = expectedUri.indexOf(suffix);

        if (suffixIndex != -1) {
            return targetUri.endsWith(expectedUri.substring(suffixIndex + suffix.length() - 1));
        }

        return false;
    }

    protected String buildUriFromTemplate(String template, String targetUri, boolean onlyFirstParam) {
        StringBuilder uri = new StringBuilder(template);
        String expectedUri = uri.toString();
        int patternStartIndex = expectedUri.indexOf("{");

        if (expectedUri.endsWith("/*")) {
            expectedUri = expectedUri.substring(0, expectedUri.length() - 2);
        }

        if (patternStartIndex == -1 || patternStartIndex >= targetUri.length()) {
            return null;
        }

        if (expectedUri.split("/").length > targetUri.split("/").length) {
            return null;
        }

        char[] expectedUriChars = expectedUri.toCharArray();
        char[] matchingUri = Arrays.copyOfRange(expectedUriChars, 0, patternStartIndex);
        int matchingUriLastIndex = matchingUri.length;
        String targetUriParams = targetUri.substring(patternStartIndex);

        if (Arrays.equals(matchingUri, Arrays.copyOf(targetUri.toCharArray(), matchingUri.length))) {
            matchingUri = Arrays.copyOf(matchingUri, targetUri.length());
            int paramIndex = 0;
            int lastPattern = 0;

            for (int i = patternStartIndex; i < expectedUriChars.length; i++) {
                if (matchingUriLastIndex >= matchingUri.length) {
                    break;
                }

                char c = expectedUriChars[i];

                if (c == '{' || c == '*') {
                    String[] params = targetUriParams.split("/");

                    for (int k = paramIndex; k <= (c == '*' ? params.length : paramIndex); k++) {
                        if (k == params.length) {
                            break;
                        }

                        int paramLength = params[k].length();

                        if (matchingUriLastIndex + paramLength > matchingUri.length) {
                            return null;
                        }
                        
                        StringBuilder value = new StringBuilder();

                        for (int j = 0; j < paramLength; j++) {
                            char valueChar = params[k].charAt(j);
                            value.append(valueChar);
                            matchingUri[matchingUriLastIndex++] = valueChar;
                        }

                        if (c == '{') {
                            int openBraceIndex = uri.indexOf("{", lastPattern);
                            int closingBraceIndex = uri.indexOf("}", lastPattern);
                            if (openBraceIndex == -1 || closingBraceIndex == -1 || closingBraceIndex < openBraceIndex) {
                                return null;
                            }
                            String paramName = uri.substring(openBraceIndex + 1, closingBraceIndex);
                            if (paramName.indexOf('/') != -1) {
                                return null;
                            }
                            uri.replace(openBraceIndex, closingBraceIndex + 1, value.toString());
                        }

                        if (value.length() > 0 && value.charAt(value.length() - 1) == '}') {
                            lastPattern = uri.indexOf(value.toString()) + value.length();
                        }

                        if (c == '*' && matchingUriLastIndex < matchingUri.length) {
                            matchingUri[matchingUriLastIndex++] = '/';
                        }
                    }

                    if (c == '{') {
                        i = expectedUri.indexOf('}', i);
                    }

                    if (i == expectedUri.lastIndexOf('}') && onlyFirstParam) {
                        return String.valueOf(matchingUri).substring(0, matchingUriLastIndex);
                    }
                } else {
                    if (c == '/') {
                        paramIndex++;
                    }
                    matchingUri[matchingUriLastIndex++] = c;
                }
            }

            return uri.toString();
        }

        return null;
    }

    public boolean endsWithWildcard(String expectedUri) {
        int length = expectedUri.length();
        return length > 0 && WILDCARD == expectedUri.charAt(length - 1);
    }

    private boolean isTemplate(String uri) {
        return uri.indexOf("{") != -1;
    }

    /**
     * Validates that the given URI is a well-formed path template.
     *
     * @return an error message if the URI is invalid, or {@code null} if it is valid
     */
    public static String validateTemplate(String uri) {
        boolean inBrace = false;
        boolean empty = true;

        for (int i = 0; i < uri.length(); i++) {
            char c = uri.charAt(i);

            if (c == '{') {
                if (inBrace) {
                    return "nested '{'";
                }
                inBrace = true;
                empty = true;
            } else if (c == '}') {
                if (!inBrace || empty) {
                    return "unexpected '}' or empty parameter name";
                }
                inBrace = false;
            } else if (inBrace) {
                if (c == '/') {
                    return "parameter name contains '/'";
                }
                empty = false;
            }
        }

        if (inBrace) {
            return "missing closing '}'";
        }

        int asteriskIndex = uri.indexOf('*');
        if (asteriskIndex != -1) {
            boolean validTrailing = uri.endsWith("/*") && asteriskIndex == uri.length() - 1;
            boolean validSuffix = asteriskIndex > 0 && uri.charAt(asteriskIndex - 1) == '/'
                    && asteriskIndex + 2 < uri.length() && uri.charAt(asteriskIndex + 1) == '.'
                    && uri.indexOf('*', asteriskIndex + 1) == -1
                    && uri.indexOf('/', asteriskIndex + 1) == -1;
            if (!validTrailing && !validSuffix) {
                return "wildcard '*' is only supported as trailing '/*' or as a suffix pattern '/*.ext'";
            }
        }

        return null;
    }

    protected P resolvePathConfig(P entry, String path) {
        return entry;
    }

    private static final String SCHEME_AUTHORITY_SEPARATOR = "://";

    protected String normalizeUri(String uri) {
        if (uri == null) {
            return null;
        }

        // drop query/fragment first, before any URI parsing - a malformed query/fragment (which we discard
        // unconditionally anyway) must not be able to poison the scheme/authority syntax probe below and fall
        // through to treating the whole value - scheme included - as a plain path
        String withoutQueryOrFragment = stripQueryAndFragment(uri);

        // resources can be configured with a full absolute URI (e.g. "https://my.domain/example") rather than
        // just a path. Detect and preserve the "scheme://authority" prefix verbatim, normalizing only the path
        // that follows - otherwise the double-slash collapsing below would corrupt the "//" that separates the
        // scheme from the authority. This is a syntax probe only: constructing a URI does not itself normalize
        // anything (no dot-segment resolution, no slash collapsing, no decoding of raw components), it merely
        // locates where the authority ends so the prefix can be sliced off by length.
        String prefix = "";
        String path = withoutQueryOrFragment;
        // cheap pre-check - avoid constructing a URI (and the associated parsing cost) for the common case of a
        // plain relative path, which can never have a scheme/authority prefix to preserve
        if (withoutQueryOrFragment.contains(SCHEME_AUTHORITY_SEPARATOR)) {
            try {
                URI parsed = new URI(withoutQueryOrFragment.replace("{", "%7B").replace("}", "%7D"));
                String scheme = parsed.getScheme();
                if (scheme != null) {
                    String authority = parsed.getRawAuthority();
                    if (authority == null) {
                        // a scheme was recognized - e.g. "https:///api/admin", where the empty authority between
                        // the double slash and the next slash comes back as null rather than "" - so this was
                        // meant to be an absolute URI. Reject it outright rather than falling back to treating
                        // the raw, unmangled "scheme://" text as a plain path
                        return null;
                    }
                    // clamp to length() - the brace-encoding above can inflate the parsed authority's length
                    // relative to the original string (each '{'/'}' becomes 3 chars), so a hypothetical brace inside
                    // the authority itself (unsupported - templates are always path-only) must not overrun it
                    int prefixLength = Math.min(scheme.length() + SCHEME_AUTHORITY_SEPARATOR.length() + authority.length(), withoutQueryOrFragment.length());
                    prefix = withoutQueryOrFragment.substring(0, prefixLength);
                    path = withoutQueryOrFragment.substring(prefixLength);
                }
                // scheme == null: not actually an absolute URI - the "://" was just incidental text inside an
                // ordinary relative path (e.g. "/api/redirect-to-https://evil.com", which RFC 3986 permits
                // unrestricted since a leading '/' can never be confused with a scheme) - fall through and treat
                // the whole value as a plain path, as before
            } catch (URISyntaxException e) {
                // the pre-check found "://", so this was meant to be an absolute URI - reject it outright rather
                // than falling back to treating the raw, unmangled "scheme://" text as a plain path
                return null;
            }
        }

        // strip matrix params — prevents bypass via /api/admin;x=1 which Servlet/JAX-RS silently ignores when routing
        StringBuilder sb = new StringBuilder(path.length());
        boolean inMatrix = false;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == ';') {
                inMatrix = true;
            } else if (c == '/') {
                inMatrix = false;
                sb.append(c);
            } else if (!inMatrix) {
                sb.append(c);
            }
        }
        String result = sb.toString();

        // collapse double slashes before URI parsing — //foo is interpreted as a URI authority, not a path
        while (result.contains("//")) {
            result = result.replace("//", "/");
        }

        // resolve dot segments and decode percent-encoding — prevents bypass via /api/foo/../admin, /api/./admin,
        // or /api/%61dmin. On the server side the decoding is a no-op (servlet container already decodes form/query
        // parameters), but on the policy enforcer side getRequestURI() preserves percent-encoding so this is needed.
        // Full decoding is safe because resource paths are always configured as plain decoded strings.
        try {
            // percent-encode curly braces before URI parsing — Keycloak uses {param} in path templates
            // and new URI() rejects them as invalid characters
            result = result.replace("{", "%7B").replace("}", "%7D");
            result = new URI(result).normalize().getPath();
            if (result == null) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }

        // collapse double slashes again — decoding %2F introduces new slashes (e.g. /api/%2Fadmin → /api//admin)
        while (result.contains("//")) {
            result = result.replace("//", "/");
        }

        // strip trailing slash — prevents bypass via /api/admin/ which routes to /api/admin on the server
        if (result.length() > 1 && result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }

        return prefix + result;
    }

    // drop query/fragment entirely — the policy enforcer never sees them (getRequestURI() excludes the query),
    // and resource identity for URI matching should not depend on request parameters. Doing this before matrix
    // param stripping also prevents a ';' from swallowing a literal '?'/'#' that follows it on the same segment.
    private static String stripQueryAndFragment(String path) {
        int queryOrFragment = path.length();
        int questionMark = path.indexOf('?');
        if (questionMark != -1) {
            queryOrFragment = questionMark;
        }
        int hash = path.indexOf('#');
        if (hash != -1 && hash < queryOrFragment) {
            queryOrFragment = hash;
        }
        return path.substring(0, queryOrFragment);
    }
}
