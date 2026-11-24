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
    private static final String SUFFIX_WILDCARD = "/*.";
    private static final String SCHEME_AUTHORITY_SEPARATOR = "://";

    public P matches(final String targetUri) {
        final String normalizedUri = normalizeUri(targetUri);
        if (normalizedUri == null) {
            return null;
        }
        P matchingPath = null;
        String matchingPattern = null;

        for (P entry : getPaths()) {
            String expectedUri = getPath(entry);

            if (expectedUri == null || expectedUri.isEmpty()) {
                continue;
            }

            boolean matches = exactMatch(expectedUri, normalizedUri);

            if (isTemplate(expectedUri)) {
                String templateUri = buildUriFromTemplate(expectedUri, normalizedUri, false);

                if (templateUri != null && resolvedMatch(expectedUri, templateUri, normalizedUri)) {
                    matches = true;
                    P resolved = resolvePathConfig(entry, normalizedUri);

                    if (resolved != null) {
                        entry = resolved;
                    }
                }
            }

            if (matches && (matchingPattern == null || isMoreSpecific(expectedUri, matchingPattern))) {
                matchingPath = entry;
                matchingPattern = expectedUri;
            }
        }

        return matchingPath;
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

        int suffixIndex = expectedUri.indexOf(SUFFIX_WILDCARD);

        if (suffixIndex != -1) {
            // the pattern is "<prefix>/*<extension>", for example "/public/*.json". Both halves have to match:
            // comparing only the extension would let "/public/*.json" match "/admin/secret.json", and for an
            // absolute pattern it would also discard the scheme and authority, letting
            // "https://trusted.example/public/*.json" match "https://evil.example/admin/secret.json".
            // A templated prefix such as "/api/{id}/" never matches literally here on purpose - the caller
            // re-checks the pattern once buildUriFromTemplate() has resolved it against the target URI.
            String prefix = expectedUri.substring(0, suffixIndex + 1);
            String extension = expectedUri.substring(suffixIndex + 2);

            // the length check keeps a target too short to hold both halves from matching the prefix and the
            // extension with the same, overlapping characters
            return targetUri.length() >= prefix.length() + extension.length()
                    && targetUri.startsWith(prefix)
                    && targetUri.endsWith(extension);
        }

        return false;
    }

    /**
     * Whether a template, once its parameters have been filled in from the target, matches that target.
     * <p>
     * Which wildcard the pattern declares, and the extension that follows it, are read from the registered
     * pattern and never from the resolved URI. A parameter is filled in from the request, so the resolved URI
     * can hold a "*" the request supplied, and looking the wildcard up there would read it as one the resource
     * declared. "/api/{id}/private/*.json" against a request whose parameter is "*.json" resolves to
     * "/api/*.json/private/*.json": its prefix then reads as "/api/" and its extension as
     * ".json/private/*.json", so it matches "/api/*.json/evil.json/private/*.json", a target that has no
     * "private" segment where the pattern puts one, and misses "/api/*.json/private/x.json", which it covers.
     * <p>
     * Only the prefix has to come from the resolved form, and it is everything up to the last segment: a
     * wildcard is always the pattern's last segment and a parameter value is always a single segment, so the
     * registered wildcard occupies the last segment of the resolved URI as well.
     */
    private boolean resolvedMatch(String pattern, String resolvedUri, String targetUri) {
        if (endsWithWildcard(pattern)) {
            // a trailing "/*" is copied into the resolved URI verbatim, so it is still its last character and
            // exactMatch() reads the wildcard from there before it ever looks for a suffix one
            return exactMatch(resolvedUri, targetUri);
        }

        int suffixIndex = pattern.indexOf(SUFFIX_WILDCARD);

        if (suffixIndex == -1) {
            // a template that declares no wildcard has to resolve to the target exactly
            return targetUri.equals(resolvedUri);
        }

        String prefix = resolvedUri.substring(0, resolvedUri.lastIndexOf('/') + 1);
        String extension = pattern.substring(suffixIndex + 2);

        return targetUri.length() >= prefix.length() + extension.length()
                && targetUri.startsWith(prefix)
                && targetUri.endsWith(extension);
    }

    /**
     * How much a single pattern segment pins down, from weakest to strongest. A wildcard accepts whatever is
     * left of the target, a parameter accepts any one segment, and a literal names exactly one value.
     */
    private static final int WILDCARD_SEGMENT = 0;
    private static final int PARAMETER_SEGMENT = 1;
    private static final int LITERAL_SEGMENT = 2;

    private int segmentStrength(String segment) {
        if (!segment.isEmpty() && segment.charAt(0) == WILDCARD) {
            return WILDCARD_SEGMENT;
        }

        return segment.indexOf('{') == -1 ? LITERAL_SEGMENT : PARAMETER_SEGMENT;
    }

    /**
     * Decides whether {@code candidate} pins the requested URI down more than {@code current} does.
     * <p>
     * Both patterns matched the same target, so they are compared segment by segment on how much each one pins
     * down, and the first position where they disagree decides. Comparing whole-pattern measures instead - a
     * prefix length, a parameter count - cannot work: each of them collapses the pattern to a single number and
     * then loses either the literal segments that follow a parameter or the depth that follows a wildcard, which
     * is how "/api/*" came to outrank "/api/{tenant}/admin/*" even though the latter matches a strict subset.
     * <p>
     * This is a ranking, not a containment test. Two patterns that match the same target are usually
     * incomparable, neither one covering a superset of the other, so there is no narrowest pattern to pick and
     * the rule below simply has to settle them. Where one of them does cover a strict superset of the other the
     * rule normally picks the narrower one, but not in every case: ranking a parameter above a wildcard is what
     * keeps "/api/{tenant}/admin/*" ahead of the "/api/*" that contains it, and the very same comparison puts
     * "/public/{dir}/*" ahead of "/public/*.json", which it also contains. The two cannot both be honoured, and
     * the nesting is deliberately respected for the first. See
     * {@code parameterSegmentIsPreferredEvenWhenTheSuffixPatternIsNarrower} in {@code PathMatcherTest}.
     * <p>
     * Only the registered patterns are compared, never their resolved forms. A resolved parameter reads as a
     * literal but holds a value the request chose, so counting it as a constraint would let the catch-all
     * "/{a}/{b}/*" pass itself off as "/admin/private/*" and outrank "/admin/*.json".
     */
    private boolean isMoreSpecific(String candidate, String current) {
        String[] candidateSegments = candidate.split("/");
        String[] currentSegments = current.split("/");
        int shared = Math.min(candidateSegments.length, currentSegments.length);

        for (int i = 0; i < shared; i++) {
            String candidateSegment = candidateSegments[i];
            String currentSegment = currentSegments[i];
            int strength = segmentStrength(candidateSegment);
            int currentStrength = segmentStrength(currentSegment);

            if (strength != currentStrength) {
                return strength > currentStrength;
            }

            // a wildcard is always the last segment of a pattern, so this is the final say: "/p/*.json" pins the
            // target down more than "/p/*" does, and "*.report.json" more than "*.json"
            if (strength == WILDCARD_SEGMENT && candidateSegment.length() != currentSegment.length()) {
                return candidateSegment.length() > currentSegment.length();
            }
        }

        // the two agree as far as the shorter one goes. Whatever the longer one adds can only be a wildcard -
        // a literal or parameter segment would need a target segment the shorter pattern has already consumed -
        // so the shorter pattern is the one that stops exactly where the target does: "/api/{v}/users" is more
        // specific than "/api/{v}/users/*"
        if (candidateSegments.length != currentSegments.length) {
            return candidateSegments.length < currentSegments.length;
        }

        return breakTie(candidate, current);
    }

    /**
     * Last resort for patterns that are equally specific by every meaningful measure, which happens when they are
     * duplicates of each other - the same URI registered twice, or templates that differ only in their parameter
     * names, such as "/api/{a}/{b}" and "/api/{version}/{resource}". Which one is chosen is arbitrary, but it must
     * not depend on the order the resources are visited in: the callers iterate a {@link java.util.HashMap}, so
     * without this the same request could resolve differently from one deployment or restart to the next.
     */
    private boolean breakTie(String pattern, String currentPattern) {
        return pattern.compareTo(currentPattern) < 0;
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
        // Only a colon before the first slash can introduce a scheme, so ordinary paths skip costly URI parsing.
        // This also catches authority-less URIs such as "https:/api/admin", while leaving colons inside paths alone.
        int colon = withoutQueryOrFragment.indexOf(':');
        int slash = withoutQueryOrFragment.indexOf('/');
        if (colon > 0 && (slash == -1 || colon < slash)) {
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
                // scheme == null: the colon did not introduce a valid scheme; treat the value as a plain path
            } catch (URISyntaxException e) {
                // a possible scheme prefix failed to parse; do not treat it as a plain path
                return null;
            }
        }

        // strip matrix params — prevents bypass via /api/admin;x=1 which Servlet/JAX-RS silently ignores when routing
        String result = stripMatrixParams(path);

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

        // repeat cleanup on decoded result - %3B decodes to ; and %2F decodes to / (introducing new // when
        // adjacent to existing slashes or after matrix param stripping removes content between two slashes)
        result = stripMatrixParams(result);
        while (result.contains("//")) {
            result = result.replace("//", "/");
        }

        // re-normalize after decoding to resolve any dot segments that were not visible
        // to the first normalize() pass.
        try {
            result = new URI(null, null, result, null, null).normalize().getPath();
            if (result == null) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }

        // URI.normalize() leaves leading parent segments unresolved. RFC 3986 section 5.2.4 removes them
        // even when there is no preceding segment to remove. Do this after decoding so encoded dots and slashes
        // are handled as well.
        while (result.startsWith("/../")) {
            result = result.substring(3);
        }
        if (result.equals("/..")) {
            result = "/";
        }

        // strip trailing slash - prevents bypass via /api/admin/ which routes to /api/admin on the server
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

    private static String stripMatrixParams(String uri) {
        StringBuilder sb = new StringBuilder(uri.length());
        boolean inMatrix = false;
        for (int i = 0; i < uri.length(); i++) {
            char c = uri.charAt(i);
            if (c == ';') {
                inMatrix = true;
            } else if (c == '/') {
                inMatrix = false;
                sb.append(c);
            } else if (!inMatrix) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
