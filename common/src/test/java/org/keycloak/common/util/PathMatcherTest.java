package org.keycloak.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PathMatcherTest {

    @Test
    public void templateWithWildcardMatchesCorrectPathOnly() {
        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList(
                "/api/v1/{clientId}/campaigns/*"
        ));

        Assertions.assertNotNull(matcher.matches("/api/v1/1/campaigns/summer"));
        Assertions.assertNull(matcher.matches("/api/v1/1/contentConnectorConfigs/29/contentConnectorContents"));
    }

    @Test
    public void testMatrixParamsStripped() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertEquals("/api/admin", matcher.matches("/api/admin;x=1"));
        Assertions.assertEquals("/api/admin", matcher.matches("/api/admin;jsessionid=abc123"));
        Assertions.assertEquals("/api/admin", matcher.matches("/api/admin;a=1;b=2"));
    }

    @Test
    public void testMatrixParamsInMiddleSegment() {
        PathMatcher<String> matcher = createMatcher("/api/admin/data", "/*");
        Assertions.assertEquals("/api/admin/data", matcher.matches("/api;v=2/admin/data"));
        Assertions.assertEquals("/api/admin/data", matcher.matches("/api/admin;x=1/data"));
    }

    @Test
    public void testTrailingSlashStripped() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertEquals("/api/admin", matcher.matches("/api/admin/"));
    }

    @Test
    public void testDoubleSlashesCollapsed() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertEquals("/api/admin", matcher.matches("/api//admin"));
        Assertions.assertEquals("/api/admin", matcher.matches("//api///admin"));
    }

    @Test
    public void testDotSegmentsResolved() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertEquals("/api/admin", matcher.matches("/api/foo/../admin"));
        Assertions.assertEquals("/api/admin", matcher.matches("/api/./admin"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/../api/admin",
            "/../../api/admin",
            "/api/../../api/admin",
            "/%2E%2E/api/admin",
            "/%2e./api/admin",
            "/.%2e/api/admin",
            "/..%2Fapi/admin",
            "https://my.domain/../api/admin",
            "https://my.domain/%2E%2E/api/admin"
    })
    public void testAboveRootDotSegmentsClampToRoot(String uri) {
        // Extra ".." segments stop at the root; the remaining path still matches the resource.
        String resource = uri.startsWith("https://") ? "https://my.domain/api/admin" : "/api/admin";
        String wildcard = uri.startsWith("https://") ? "https://my.domain/*" : "/*";
        PathMatcher<String> matcher = createMatcher(resource, wildcard);

        Assertions.assertEquals(resource, matcher.matches(uri), "Should clamp to the root for: " + uri);
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "/..", 
      "/../", 
      "/../../", 
      "/%2E%2E", 
      "https://my.domain/.."
    })
    public void testAboveRootDotSegmentsResolveToRoot(String uri) {
        // When nothing follows the extra ".." segments, the result is the root itself.
        String root = uri.startsWith("https://") ? "https://my.domain/" : "/";
        PathMatcher<String> matcher = createMatcher(root);

        Assertions.assertEquals(root, matcher.matches(uri), "Should resolve to the root for: " + uri);
    }

    @Test
    public void testPercentEncodedUnreservedDecoded() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertEquals("/api/admin", matcher.matches("/api/%61dmin"));
        Assertions.assertEquals("/api/admin", matcher.matches("/%61pi/%61dmin"));
    }

    @Test
    public void testPercentEncodedReservedDecoded() {
        PathMatcher<String> matcher = createMatcher("/api/admin area", "/*");
        Assertions.assertEquals("/api/admin area", matcher.matches("/api/admin%20area"));
    }

    @Test
    public void testPercentEncodedSemicolonStripped() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertEquals("/api/admin", matcher.matches("/api/admin%3Bx=1"));
        Assertions.assertEquals("/api/admin", matcher.matches("/api/admin%3bx=1"));
    }

    @Test
    public void testPercentEncodedSemicolonInMiddleSegment() {
        PathMatcher<String> matcher = createMatcher("/api/admin/data", "/*");
        Assertions.assertEquals("/api/admin/data", matcher.matches("/api/admin%3Bx=1/data"));
    }

    @Test
    public void testPercentEncodedSlashDecoded() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertEquals("/api/admin", matcher.matches("/api/%2Fadmin"));
    }

    @Test
    public void testPercentEncodedDotSegmentsResolved() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertEquals("/api/admin", matcher.matches("/api/foo%2F..%2Fadmin"));
        Assertions.assertEquals("/api/admin", matcher.matches("/api/foo/%2E%2E/admin"));
        Assertions.assertEquals("/api/admin", matcher.matches("/api/foo%2F..%2F.%2Fadmin"));
        Assertions.assertEquals("/api/admin", matcher.matches("/api/x%2F..%2Fy%2F..%2Fadmin"));
        Assertions.assertEquals("/api/admin", matcher.matches("/api/x/%2e%2e/admin"));
        Assertions.assertEquals("/api/admin", matcher.matches("/api/x/%2e./admin"));
        Assertions.assertEquals("/api/admin", matcher.matches("/api/x/.%2e/admin"));
    }

    @Test
    public void testPercentEncodedDotCurrentDirResolved() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertEquals("/api/admin", matcher.matches("/api/%2E/admin"));
        Assertions.assertEquals("/api/admin", matcher.matches("/api/.%2Fadmin"));
    }

    @Test
    public void testPercentEncodedTrailingSlashStripped() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertEquals("/api/admin", matcher.matches("/api/admin%2F"));
    }

    @Test
    public void testPercentEncodedDoubleSlashCollapsed() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertEquals("/api/admin", matcher.matches("/api%2F%2Fadmin"));
        Assertions.assertEquals("/api/admin", matcher.matches("/api%2Fadmin"));
    }

    @Test
    public void testDoubleEncodedSlashNotFurtherDecoded() {
        // a double-encoded slash ("%252F") decodes exactly once, to the literal text "%2F" - it must not be
        // decoded a second time into a real path separator. Servlet containers only ever decode one layer, so
        // decoding further here would mismatch what the container actually routes to.
        PathMatcher<String> matcher = createMatcher("/api/%2Fadmin", "/api/admin", "/*");
        Assertions.assertEquals("/api/%2Fadmin", matcher.matches("/api/%252Fadmin"));
    }

    @Test
    public void testDoubleEncodedSlashDoesNotMatchSingleDecodedResource() {
        // consequently, a resource configured with the plain decoded path must NOT match a double-encoded
        // slash - it falls through to the wildcard instead, it does not resolve to "/api/admin"
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertEquals("/*", matcher.matches("/api/%252Fadmin"));
    }

    @Test
    public void testDoubleEncodedSlashDoesNotProduceUnresolvedDoubleSlash() {
        // the second (post-decode) normalize() pass uses the 5-arg URI(scheme, authority, path, query, fragment)
        // constructor, which always re-quotes a literal '%' when building the raw path, so getPath() can never
        // re-decode text that already went through one decode pass - this must never contain "//"
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        String result = matcher.matches("/api/%252Fadmin");
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.contains("//"), "Unexpected unresolved double slash in: " + result);
    }

    @Test
    public void testDoubleEncodedDotSegmentNotFurtherDecoded() {
        // same reasoning as above, for a double-encoded dot segment ("%252E%252E" decodes once to the literal
        // text "%2E%2E", not to ".."), so it must not be resolved as a dot segment by the second normalize() pass
        PathMatcher<String> matcher = createMatcher("/api/foo/%2E%2E/admin", "/api/admin", "/*");
        Assertions.assertEquals("/api/foo/%2E%2E/admin", matcher.matches("/api/foo/%252E%252E/admin"));
    }

    @Test
    public void testCombinedVectors() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertEquals("/api/admin", matcher.matches("/api/admin;x=1/"));
        Assertions.assertEquals("/api/admin", matcher.matches("//api/foo/../admin;y=2/"));
        Assertions.assertEquals("/api/admin", matcher.matches("/api/./admin;session=abc"));
    }

    @Test
    public void testAbsoluteUriSchemeAndAuthorityPreserved() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/example", "/*");
        Assertions.assertEquals("https://my.domain/example", matcher.matches("https://my.domain/example"));
    }

    @Test
    public void testAbsoluteUriWithPortPreserved() {
        PathMatcher<String> matcher = createMatcher("https://my.domain:8080/example", "/*");
        Assertions.assertEquals("https://my.domain:8080/example", matcher.matches("https://my.domain:8080/example"));
    }

    @Test
    public void testAbsoluteUriMatchesTemplate() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/example/{module-name}", "/*");
        Assertions.assertEquals("https://my.domain/example/{module-name}", matcher.matches("https://my.domain/example/one"));
    }

    @Test
    public void testAbsoluteUriMatchesWildcard() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/example/*", "/other");
        Assertions.assertEquals("https://my.domain/example/*", matcher.matches("https://my.domain/example/one"));
    }

    @Test
    public void testAbsoluteUriMatrixParamsStripped() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/api/admin", "/*");
        Assertions.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api/admin;x=1"));
    }

    @Test
    public void testAbsoluteUriPercentEncodedSemicolonStripped() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/api/admin", "/*");
        Assertions.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api/admin%3Bx=1"));
        Assertions.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api/admin%3bx=1"));
    }

    @Test
    public void testAbsoluteUriDotSegmentsResolved() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/api/admin", "/*");
        Assertions.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api/foo/../admin"));
    }

    @Test
    public void testAbsoluteUriPercentEncodedDotSegmentsResolved() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/api/admin", "/*");
        Assertions.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api/foo/%2E%2E/admin"));
        Assertions.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api/foo%2F..%2Fadmin"));
    }

    @Test
    public void testAbsoluteUriDoubleSlashesInPathCollapsed() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/api/admin", "/*");
        Assertions.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api//admin"));
    }

    @Test
    public void testAbsoluteUriTrailingSlashStripped() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/api/admin", "/*");
        Assertions.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api/admin/"));
    }

    @Test
    public void testQueryStringDropped() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertEquals("/api/admin", matcher.matches("/api/admin?x=1"));
    }

    @Test
    public void testFragmentDropped() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertEquals("/api/admin", matcher.matches("/api/admin#section"));
    }

    @Test
    public void testQueryStringNotSplicedAcrossMatrixParam() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertEquals("/api/admin", matcher.matches("/api/admin;x=1?redirect=/evil"));
    }

    @Test
    public void testAbsoluteUriQueryStringDropped() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/api/admin", "/*");
        Assertions.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api/admin?x=1"));
    }

    @Test
    public void testQueryWithNestedSchemeAndSlashesDropped() {
        // a query value containing its own "://" and "//" must not leak into, or be confused with, the kept prefix
        PathMatcher<String> matcher = createMatcher("https://host/path", "/*");
        Assertions.assertEquals("https://host/path", matcher.matches("https://host/path?x=https://a//b"));
    }

    @Test
    public void testMalformedQueryDoesNotCorruptSchemeAndAuthority() {
        // a malformed %-escape in the (discarded) query must not poison the scheme/authority syntax probe and
        // fall through to mangling "https://" as if it were plain path content
        PathMatcher<String> matcher = createMatcher("https://my.domain/api/admin", "/*");
        Assertions.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api/admin?bad=%"));
    }

    @Test
    public void testEncodedQuestionMarkNotTreatedAsDelimiter() {
        // %3F is not a literal '?', so it must not be truncated as a query delimiter - it survives as part of
        // the path and gets decoded normally by the existing single-decode pass, same as any other %-octet
        PathMatcher<String> matcher = createMatcher("/api/search?foo", "/*");
        Assertions.assertEquals("/api/search?foo", matcher.matches("/api/search%3Ffoo"));
    }

    @Test
    public void testRootPathPreserved() {
        PathMatcher<String> matcher = createMatcher("/", "/api/admin");
        Assertions.assertEquals("/", matcher.matches("/"));
    }

    @Test
    public void testNormalUriUnchanged() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertEquals("/api/admin", matcher.matches("/api/admin"));
    }

    @Test
    public void testWildcardMatchingStillWorks() {
        PathMatcher<String> matcher = createMatcher("/api/admin/*", "/other");
        Assertions.assertEquals("/api/admin/*", matcher.matches("/api/admin/sub"));
        Assertions.assertEquals("/api/admin/*", matcher.matches("/api/admin/sub/deep"));
    }

    @Test
    public void testTemplateMatchingStillWorks() {
        PathMatcher<String> matcher = createMatcher("/api/{id}/items", "/*");
        Assertions.assertEquals("/api/{id}/items", matcher.matches("/api/123/items"));
    }

    @Test
    public void testCurlyBracesInTargetUri() {
        PathMatcher<String> matcher = createMatcher("/rest/{version}/carts/{cartId}/cartactions/{actionId}", "/*");
        Assertions.assertNotNull(matcher.matches("/rest/v2/carts/{cartId}/cartactions/123"));
    }

    @Test
    public void testMalformedUriReturnsNoMatch() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertNull(matcher.matches("/api/foo/../admin%"));
        Assertions.assertNull(matcher.matches("/api/admin%"));
    }

    @Test
    public void testUriWithNoPathReturnsNoMatch() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertNull(matcher.matches("foo:bar"));
    }

    @Test
    public void testMalformedAbsoluteUriReturnsNoMatch() {
        // a string that looks like an absolute URI (contains "://") but fails to parse as one must be rejected
        // outright, not fall back to treating the raw "scheme://" text as a plain path
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertNull(matcher.matches("https://[not-ipv6]/api/admin"));
    }

    @Test
    public void testAuthorityLessAbsoluteUriReturnsNoMatch() {
        // "https:///api/admin" has a recognized scheme but the empty authority comes back as null (not "") -
        // must be rejected outright, not fall back to treating "https://" as plain path content
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertNull(matcher.matches("https:///api/admin"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https:/api/admin",
            "http:/api/admin",
            "HTTPS:/api/admin",
            "https:/api/%61dmin",
            "https:/api/admin?x=1",
            "https:/api/admin#section"
    })
    public void testSingleSlashSchemeUriReturnsNoMatch(String uri) {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");

        Assertions.assertNull(matcher.matches(uri), "Should reject authority-less scheme URI: " + uri);
    }

    @Test
    public void testIncidentalSchemeSeparatorInRelativePathStillNormalized() {
        // "://" embedded in an ordinary relative path (no recognized scheme, since it doesn't start with one) is
        // not an absolute URI at all - it must still fall through to plain path normalization, not be rejected
        PathMatcher<String> matcher = createMatcher("/api/redirect-to-https:/example.com", "/other");
        Assertions.assertEquals("/api/redirect-to-https:/example.com", matcher.matches("/api/redirect-to-https://example.com"));
    }

    @Test
    public void testMutatedUriDoesNotFallThroughToWildcard() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");

        for (String mutated : Arrays.asList(
                "/api/admin;x=1",
                "/api/admin/",
                "/api//admin",
                "/api/foo/../admin",
                "/api/%61dmin",
                "/api/%2Fadmin",
                "/api/admin%3Bx=1",
                "/api/admin%3bx=1",
                "/api/foo%2F..%2Fadmin",
                "/api/foo/%2E%2E/admin",
                "/api/x/%2e%2e/admin",
                "/api/x/%2e./admin",
                "/api/x/.%2e/admin",
                "/api/admin%2F",
                "/api%2F%2Fadmin",
                "/api/admin;x=1/",
                "//api/foo/../admin;y=2/"
        )) {
            String result = matcher.matches(mutated);
            Assertions.assertNotNull(result, "Should match for: " + mutated);
            Assertions.assertEquals("/api/admin", result,
                    "Should match /api/admin, not /* for: " + mutated);
        }
    }

    private PathMatcher<String> createMatcher(String... paths) {
        Map<String, String> pathMap = new HashMap<>();
        for (String p : paths) {
            pathMap.put(p, p);
        }
        return new PathMatcher<String>() {
            @Override
            protected String getPath(String entry) {
                return entry;
            }

            @Override
            protected Collection<String> getPaths() {
                return pathMap.values();
            }
        };
    }

    @Test
    public void missingClosingBraceShouldReturnNull() {
        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList(
                "/api/{clientId"
        ));

        Assertions.assertNull(matcher.matches("/api/123"));
    }

    @Test
    public void strayClosingBraceBetweenTwoParamsShouldNotThrow() {
        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList(
                "/api/{p1}/}/c/{p2}"
        ));

        Assertions.assertNull(matcher.matches("/api/1/2/c/3"));
    }

    @Test
    public void strayClosingBraceEmbeddedBetweenTwoParamsShouldNotThrow() {
        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList(
                "/api/{p1}/x}y/{p2}"
        ));

        Assertions.assertNull(matcher.matches("/api/1/x}y/2"));
    }

    @Test
    public void doubleClosingBraceBetweenTwoParamsShouldNotThrow() {
        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList(
                "/api/{p1}}/{p2}"
        ));

        Assertions.assertNull(matcher.matches("/api/1/2"));
    }

    @Test
    public void strayClosingBraceAfterAllParamsIsLiteral() {
        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList(
                "/api/{p1}/foo}/*"
        ));

        Assertions.assertNull(matcher.matches("/api/1/foo/anything"));
        Assertions.assertNotNull(matcher.matches("/api/1/foo}/anything"));
    }

    @Test
    public void strayClosingBraceBeforeFirstParamShouldNotMatch() {
        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList(
                "/api/}/{p1}/*"
        ));

        Assertions.assertNull(matcher.matches("/api/}/1/anything"));
    }

    @Test
    public void emptyPlaceholderMatchesForLegacyCompatibility() {
        String template = "/api/{}/foo";
        Assertions.assertNotNull(PathMatcher.validateTemplate(template));

        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList(template));
        Assertions.assertNotNull(matcher.matches("/api/1/foo"));
    }

    @Test
    public void emptyExtensionSuffixMatchesForLegacyCompatibility() {
        String template = "/*.";
        Assertions.assertNotNull(PathMatcher.validateTemplate(template));

        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList(template));
        Assertions.assertNotNull(matcher.matches("/file."));
        Assertions.assertNull(matcher.matches("/file.txt"));
    }

    @Test
    public void slashInsidePlaceholderWithTrailingWildcard() {
        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList(
                "/api/{a/b}/*"
        ));

        Assertions.assertNull(matcher.matches("/api/1/anything"));
        Assertions.assertNull(matcher.matches("/api/1/x/y/z"));
    }

    @Test
    public void slashInsidePlaceholderWithoutWildcard() {
        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList(
                "/api/{a/b}/foo"
        ));

        // inflated segment count makes this unmatchable
        Assertions.assertNull(matcher.matches("/api/1/foo"));
        Assertions.assertNull(matcher.matches("/api/1/b/foo"));
    }

    @Test
    public void validateTemplateAcceptsValidPatterns() {
        Assertions.assertNull(PathMatcher.validateTemplate("/api/foo"));
        Assertions.assertNull(PathMatcher.validateTemplate("/api/{id}/info"));
        Assertions.assertNull(PathMatcher.validateTemplate("/api/{id}/*"));
        Assertions.assertNull(PathMatcher.validateTemplate("/*"));
        Assertions.assertNull(PathMatcher.validateTemplate("/*.html"));
        Assertions.assertNull(PathMatcher.validateTemplate("/api/{id}/*.json"));
    }

    @Test
    public void validateTemplateRejectsMalformedBraces() {
        Assertions.assertNotNull(PathMatcher.validateTemplate("/api/{id"));
        Assertions.assertNotNull(PathMatcher.validateTemplate("/api/id}"));
        Assertions.assertNotNull(PathMatcher.validateTemplate("/api/{}"));
        Assertions.assertNotNull(PathMatcher.validateTemplate("/api/{{id}}"));
        Assertions.assertNotNull(PathMatcher.validateTemplate("/api/{a/b}"));
    }

    @Test
    public void validateTemplateRejectsMalformedWildcards() {
        Assertions.assertNotNull(PathMatcher.validateTemplate("/api/*/info"));
        Assertions.assertNotNull(PathMatcher.validateTemplate("/api/*/info/*"));
        Assertions.assertNotNull(PathMatcher.validateTemplate("/a/b/c/*.html/c/d"));
        Assertions.assertNotNull(PathMatcher.validateTemplate("/*."));
    }

    @Test
    public void emptyTemplateShouldNotMatch() {
        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList(
                ""
        ));

        Assertions.assertNull(matcher.matches("/foo"));
        Assertions.assertNull(matcher.matches(""));
    }

    @Test
    public void templateStartingWithPlaceholderShouldNotMatch() {
        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList(
                "{id}/foo"
        ));

        Assertions.assertNull(matcher.matches("/1/foo"));
    }

    // --- suffix wildcard patterns ("/prefix/*.ext") -------------------------------------------------------------
    //
    // The matcher used to compare only the part of the pattern from '*' onward, so "/public/*.json" matched any
    // URI ending in ".json" regardless of its prefix, scheme or authority, and a suffix pattern structurally
    // outranked a narrower trailing-wildcard one.

    @Test
    public void suffixWildcardRequiresItsLiteralPrefix() {
        assertResolvesTo("/admin/*", "/admin/secret.json", "/public/*.json", "/admin/*");

        // the pattern still covers everything below its own prefix, at any depth
        assertResolvesTo("/public/*.json", "/public/a.json", "/public/*.json", "/admin/*");
        assertResolvesTo("/public/*.json", "/public/sub/a.json", "/public/*.json", "/admin/*");
    }

    @Test
    public void suffixWildcardRequiresMatchingSchemeAndAuthority() {
        assertResolvesTo("https://evil.example/admin/*", "https://evil.example/admin/secret.json",
                "https://trusted.example/public/*.json", "https://evil.example/admin/*");

        assertResolvesTo("https://trusted.example/public/*.json", "https://trusted.example/public/a.json",
                "https://trusted.example/public/*.json", "https://evil.example/admin/*");
    }

    @Test
    public void templatedSuffixWildcardRequiresPrefixAndParameterSegment() {
        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList("/api/{id}/*.json"));

        Assertions.assertEquals("/api/{id}/*.json", matcher.matches("/api/123/x.json"));
        Assertions.assertNull(matcher.matches("/totally/unrelated/evil.json"));
        Assertions.assertNull(matcher.matches("/api/evil.json"));
    }

    @Test
    public void narrowerTrailingWildcardBeatsBroaderSuffixWildcard() {
        // the suffix pattern genuinely matches here - its prefix is an ancestor of the trailing wildcard's - so
        // only the specificity rule keeps the resource that actually guards this path from being passed over
        assertResolvesTo("/public/private/*", "/public/private/secret.json",
                "/public/*.json", "/public/private/*");

        assertResolvesTo("/admin/*", "/admin/secret.json", "/*.json", "/admin/*");
    }

    @Test
    public void mostSpecificSuffixWildcardWins() {
        assertResolvesTo("/a/b/*.json", "/a/b/x.json", "/a/*.json", "/a/b/*.json");
    }

    @Test
    public void suffixWildcardBeatsTrailingWildcardSharingItsPrefix() {
        // same literal prefix, so the pattern that constrains the target more wins
        assertResolvesTo("/public/*.json", "/public/a.json", "/public/*", "/public/*.json");
        assertResolvesTo("/public/*", "/public/a.txt", "/public/*", "/public/*.json");
    }

    @Test
    public void literalUriBeatsWildcardRootMatch() {
        assertResolvesTo("/public", "/public", "/public", "/public/*");
    }

    @Test
    public void bareSuffixWildcardKeepsDocumentedBehaviour() {
        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList("/*.html"));

        Assertions.assertEquals("/*.html", matcher.matches("/page.html"));
        Assertions.assertEquals("/*.html", matcher.matches("/some/path/report.html"));
        Assertions.assertNull(matcher.matches("/page.json"));
    }

    @Test
    public void suffixWildcardDoesNotMatchBarePrefixOrAbsoluteTarget() {
        Assertions.assertNull(new TestingPathMatcher(Collections.singletonList("/public/*.json")).matches("/public"));

        // a path-only pattern does not match an absolute target, consistent with every other pattern kind
        Assertions.assertNull(new TestingPathMatcher(Collections.singletonList("/*.html")).matches("https://host/page.html"));
    }

    @Test
    public void malformedSuffixWildcardDoesNotOvermatch() {
        // patterns stored before validateTemplate() existed, and policy-enforcer configs, are never validated
        Assertions.assertNull(new TestingPathMatcher(Collections.singletonList("/a/*.x/b")).matches("/unrelated/thing.x/b"));
        Assertions.assertNull(new TestingPathMatcher(Collections.singletonList("/a/*.j/c/*.json")).matches("/evil/x.json"));
    }

    // --- suffix wildcards must not reopen the normalization bypasses ---------------------------------------------
    //
    // normalizeUri() decodes and canonicalizes the target before matching. These pin that the prefix comparison
    // sees that canonical form, so a resource guarded by a suffix pattern cannot be slipped past by encoding the
    // prefix, and an encoded payload cannot be steered onto an unrelated suffix resource.

    @Test
    public void suffixWildcardPrefixIsComparedAgainstTheNormalizedTarget() {
        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList("/admin/*.json"));

        Assertions.assertEquals("/admin/*.json", matcher.matches("/%61dmin/secret.json"));
        Assertions.assertEquals("/admin/*.json", matcher.matches("/admin;x=1/secret.json"));
        Assertions.assertEquals("/admin/*.json", matcher.matches("/admin//secret.json"));
        Assertions.assertEquals("/admin/*.json", matcher.matches("/api/../admin/secret.json"));
        Assertions.assertEquals("/admin/*.json", matcher.matches("/admin/./secret.json"));
        Assertions.assertEquals("/admin/*.json", matcher.matches("/api/%2E%2E/admin/secret.json"));
    }

    @Test
    public void mutatedTargetCannotBeSteeredOntoAnUnrelatedSuffixResource() {
        String[] paths = {"/public/*.json", "/admin/*"};

        assertResolvesTo("/admin/*", "/%61dmin/secret.json", paths);
        assertResolvesTo("/admin/*", "/admin;x=1/secret.json", paths);
        assertResolvesTo("/admin/*", "/public/../admin/secret.json", paths);
        assertResolvesTo("/admin/*", "/public%2F..%2Fadmin/secret.json", paths);
        assertResolvesTo("/admin/*", "/admin/secret.json?x=/public/a.json", paths);
        assertResolvesTo("/admin/*", "/admin/secret.json#/public/a.json", paths);
    }

    // --- template specificity must not depend on iteration order ------------------------------------------------
    //
    // Template eligibility used to be filtered by a segment/parameter count carried over from entries visited
    // earlier in the loop, so whether a template was considered at all depended on what preceded it.

    @Test
    public void templateWithFewerParametersWinsRegardlessOfOrder() {
        assertResolvesTo("/api/admin/{b}", "/api/admin/secret", "/api/{a}/{b}", "/api/admin/{b}");
    }

    @Test
    public void literalPrefixBeatsSubstitutedParameterOfTheSameLength() {
        // "/api/{id}/*.json" expands to "/api/admin/*.json", which only looks as specific as "/api/admin/*" -
        // the resource does not actually constrain that segment, so it must not outrank the literal one
        assertResolvesTo("/api/admin/*", "/api/admin/secret.json", "/api/{id}/*.json", "/api/admin/*");
    }

    @Test
    public void fullyLiteralPatternBeatsParameterizedExactMatch() {
        // "/{a}/{b}" resolves to the target exactly, but it constrains nothing, while "/admin/secret/*" spells
        // the whole requested path out. Preferring the resolved template would let a catch-all resource answer
        // for a path that a literal resource guards
        assertResolvesTo("/admin/secret/*", "/admin/secret", "/{a}/{b}", "/admin/secret/*");
        assertResolvesTo("/api/v1/users/*", "/api/v1/users", "/api/{version}/{resource}", "/api/v1/users/*");

        // with the same number of parameters the stronger kind of match wins again
        assertResolvesTo("/api/{v}/users", "/api/v1/users", "/api/{v}/users", "/api/{v}/users/*");
    }

    @Test
    public void competingWildcardTemplatesResolveDeterministically() {
        assertResolvesTo("/api/{id}/*.json", "/api/123/x.json", "/api/{id}/*", "/api/{id}/*.json");
        assertResolvesTo("/api/{id}/*.report.json", "/api/123/x.report.json",
                "/api/{id}/*.json", "/api/{id}/*.report.json");
    }

    @Test
    public void requestSuppliedWildcardCannotTurnATemplateIntoAWildcardResource() {
        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList("/api/{id}"));

        // a parameter is filled in from the request, so a request carrying a literal "*" would otherwise resolve
        // "/api/{id}" to "/api/*" and let the request pick up a resource whose pattern does not cover it at all
        Assertions.assertNull(matcher.matches("/api/*/secret"));
        Assertions.assertNull(matcher.matches("/api/*.json/secret.json"));

        // "*" is still an ordinary parameter value when the pattern really does cover the request
        Assertions.assertEquals("/api/{id}", matcher.matches("/api/*"));
        assertResolvesTo("/api/{id}/secret", "/api/*/secret", "/api/{id}", "/api/{id}/secret");
    }

    @Test
    public void requestSuppliedWildcardCannotDisplaceTheRegisteredSuffixWildcard() {
        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList("/api/{id}/private/*.json"));

        // a request supplying "*.json" as the parameter resolves this to "/api/*.json/private/*.json". Reading
        // the wildcard off the resolved URI finds that value before the registered one, leaving the pattern with
        // the prefix "/api/" and the extension ".json/private/*.json" - which matches a target whose third
        // segment is not "private" at all, and misses one where it is
        Assertions.assertNull(matcher.matches("/api/*.json/evil.json/private/*.json"));
        Assertions.assertNull(matcher.matches("/api/*.json/evil.json/x.json"));
        Assertions.assertNull(matcher.matches("/api/1/evil/x.json"));

        // "*" is still an ordinary parameter value when the pattern really does cover the request
        Assertions.assertEquals("/api/{id}/private/*.json", matcher.matches("/api/*.json/private/x.json"));
        Assertions.assertEquals("/api/{id}/private/*.json", matcher.matches("/api/1/private/x.json"));

        // so the resource the request does belong to is the one that answers for it
        assertResolvesTo("/api/{id}/evil.json/*", "/api/*.json/evil.json/private/*.json",
                "/api/{id}/private/*.json", "/api/{id}/evil.json/*");

        // the trailing-wildcard form keeps its own wildcard in the last segment, so it was never affected
        TestingPathMatcher trailing = new TestingPathMatcher(Collections.singletonList("/api/{id}/private/*"));

        Assertions.assertEquals("/api/{id}/private/*", trailing.matches("/api/*/private/x"));
        Assertions.assertNull(trailing.matches("/api/*/evil/x"));
        Assertions.assertNull(trailing.matches("/api/*.json/evil/x"));
    }

    @Test
    public void parameterSegmentIsPreferredEvenWhenTheSuffixPatternIsNarrower() {
        // ranking a parameter above a wildcard is what keeps "/api/{tenant}/admin/*" ahead of the "/api/*" that
        // contains it, and the same comparison runs here, where "/public/*.json" is itself contained in
        // "/public/{dir}/*": every URI the suffix pattern covers has a segment after "/public/". The two cannot
        // both be honoured, so the outcome is pinned here as a recorded trade-off rather than left to chance
        assertResolvesTo("/public/{dir}/*", "/public/a.json", "/public/*.json", "/public/{dir}/*");
        assertResolvesTo("/{a}/*", "/x.json", "/*.json", "/{a}/*");
    }

    @Test
    public void parameterizedPrefixNeverOutranksALiteralOneAtAnyDepth() {
        // "/{a}/{b}/*" resolves to "/admin/private/*", whose prefix is longer than "/admin/" only because both
        // segments were copied out of the request. A catch-all must not take the admin resource's place
        assertResolvesTo("/admin/*.json", "/admin/private/secret.json", "/{a}/{b}/*", "/admin/*.json");
        assertResolvesTo("/admin/*.json", "/admin/secret.json", "/{a}/{b}/*", "/admin/*.json");
        assertResolvesTo("/admin/private/*.json", "/admin/private/secret.json",
                "/{a}/{b}/*", "/admin/private/*.json");
    }

    @Test
    public void templatedWildcardRootCompetesOnSpecificity() {
        // the wildcard root of "/api/{id}/private/*" is only visible once the template is resolved. Reading it
        // from the unresolved pattern stops at the first parameter, leaving "/api/" and handing the request to
        // the broader "/api/{a}/{b}" despite its extra parameter
        assertResolvesTo("/api/{id}/private/*", "/api/123/private", "/api/{id}/private/*", "/api/{a}/{b}");
        assertResolvesTo("/api/{id}/private", "/api/123/private",
                "/api/{id}/private/*", "/api/{a}/{b}", "/api/{id}/private");
    }

    @Test
    public void parameterSegmentBeatsWildcardSegmentAtTheSamePosition() {
        // a parameter accepts one segment, a wildcard accepts everything left, so the parameter is the stronger
        // constraint. This is what keeps a deep template ahead of a shallow catch-all that shares its prefix:
        // "/api/{tenant}/admin/*" matches a strict subset of "/api/*" and must win
        assertResolvesTo("/api/{tenant}/admin/*", "/api/acme/admin/secret.json",
                "/api/*", "/api/{tenant}/admin/*");
        assertResolvesTo("/api/{tenant}/admin/*.json", "/api/acme/admin/secret.json",
                "/api/*", "/api/{tenant}/admin/*.json");
        assertResolvesTo("/{tenant}/admin/*", "/acme/admin/x", "/*", "/{tenant}/admin/*");

        // the same rule decides between two templates that are incomparable - neither matches a superset of the
        // other - so the outcome here is a deliberate consequence of ranking parameters above wildcards, not a
        // preference for "/api/{a}/{b}" as such. Inverting it would reopen the strict-subset cases above
        assertResolvesTo("/api/{a}/{b}", "/api/123/x.json", "/api/{a}/{b}", "/api/{id}/*.json");
    }

    @Test
    public void literalSegmentFollowingAParameterStillCounts() {
        // whole-pattern measures that stop at the first parameter cannot see "sub-resources" or "admin" here
        assertResolvesTo("/resources/{a}/sub-resources/{b}/*", "/resources/a/sub-resources/c/d",
                "/resources/*", "/resources/{a}/{b}/*", "/resources/{a}/sub-resources/{b}/*");
        assertResolvesTo("/api/{tenant}/admin/*", "/api/acme/admin/x",
                "/api/{tenant}/admin/*", "/api/{a}/{b}/{c}");
    }

    /**
     * The resource set registered by {@code ResourceManagementWithAuthzClientTest#testFindMatchingUri}, which is
     * the long-standing contract for how Authorization Services picks a resource for a URI. Replaying it here
     * catches a ranking regression in milliseconds instead of in an integration job.
     */
    @Test
    public void matchesTheResourceSetAssertedByTheAuthzClientIntegrationTest() {
        List<String> uris = Arrays.asList(
                "/*", "/resources/*", "/resources-a/*", "/resources-b/{pattern}", "/resources-c/{pattern}/*",
                "/resources/{pattern}/{pattern}/*", "/resources/{pattern}/sub-resources/{pattern}/*",
                "/rest/{version}/loader/loadTwo", "/rest/{version}/loader/load",
                "/rest/{version}/carts/{cartId}/cartactions/{actionId}",
                "/rest/v1/carts/{cartId}/cartactions/123",
                "/dummy/605dc7ff310256017a2ec84f", "/dummy/605dc7ff310256017a2ec84f/*");
        Map<String, String> expected = new LinkedHashMap<>();

        expected.put("/test", "/*");
        expected.put("/resources-a/test", "/resources-a/*");
        expected.put("/resources", "/resources/*");
        expected.put("/resources/", "/resources/*");
        expected.put("/resources-b/a", "/resources-b/{pattern}");
        expected.put("/resources-c/a/b", "/resources-c/{pattern}/*");
        expected.put("/resources/a/b/c", "/resources/{pattern}/{pattern}/*");
        expected.put("/resources/a/sub-resources/c/d", "/resources/{pattern}/sub-resources/{pattern}/*");
        expected.put("/rest/v1/loader/load", "/rest/{version}/loader/load");
        expected.put("/rest/v1/loader/loadTwo", "/rest/{version}/loader/loadTwo");
        expected.put("/rest/v1/carts/123/cartactions/456", "/rest/{version}/carts/{cartId}/cartactions/{actionId}");
        expected.put("/rest/v1/carts/123/cartactions/123", "/rest/v1/carts/{cartId}/cartactions/123");

        for (Map.Entry<String, String> entry : expected.entrySet()) {
            Assertions.assertEquals(entry.getValue(), new TestingPathMatcher(uris).matches(entry.getKey()),
                    "wrong resource for " + entry.getKey());
        }
    }

    /**
     * Resolution must never depend on the order in which resources are visited, whatever combination of patterns
     * is configured. The real callers feed the matcher from a {@link HashMap}, so an order-dependent rule shows up
     * in production as a resource that resolves differently from one deployment to the next.
     */
    @Test
    public void resolutionIsIndependentOfIterationOrder() {
        String[] patterns = {
                "/*", "/*.json", "/public/*", "/public/*.json", "/public/private/*", "/admin/*", "/admin/*.json",
                "/admin", "/public", "/api/{id}", "/api/{id}/*", "/api/{id}/*.json", "/api/{a}/{b}",
                "/api/admin/{b}", "/api/admin/*", "/api/{id}/*.report.json", "/users/{id}/files/*.json",
                "/{a}/{b}", "/admin/secret/*", "/api/{version}/{resource}", "/api/v1/users/*", "/api/{v}/users",
                "/{a}/{b}/*", "/{a}/*", "/{a}/*.json", "/api/{id}/private", "/api/{id}/private/*",
                "/admin/private/*", "/admin/private/*.json",
                // patterns whose literal content sits after a parameter, and deep templates competing with
                // shallow catch-alls sharing their prefix
                "/api/{tenant}/admin/*", "/api/{tenant}/admin/*.json", "/api/{a}/{b}/{c}", "/{a}/admin/*",
                "/resources/*", "/resources/{a}/{b}/*", "/resources/{a}/sub-resources/{b}/*",
        };
        String[] targets = {
                "/admin/secret.json", "/public/a.json", "/public/private/secret.json", "/public", "/admin",
                "/api/123", "/api/123/x.json", "/api/admin/secret", "/api/admin/secret.json",
                "/api/123/x.report.json", "/users/7/files/r.json", "/page.html", "/", "/api/admin/x.json",
                "/admin/secret", "/api/v1/users", "/api/v1/users/profile",
                "/admin/private/secret.json", "/admin/private/secret", "/api/123/private",
                // a request may carry a literal "*", which must not be substituted into a template and then
                // read back as a wildcard the resource never declared
                "/api/*", "/api/*/secret", "/api/*.json/secret.json", "/*/admin", "/admin/*/x",
                "/api/acme/admin/secret.json", "/api/acme/admin", "/acme/admin/x",
                "/resources/a/b/c", "/resources/a/sub-resources/c/d",
        };

        List<String> unstable = new ArrayList<>();

        for (int i = 0; i < patterns.length; i++) {
            for (int j = i + 1; j < patterns.length; j++) {
                for (int k = j + 1; k < patterns.length; k++) {
                    List<String> paths = Arrays.asList(patterns[i], patterns[j], patterns[k]);

                    for (String target : targets) {
                        String first = null;

                        for (List<String> ordering : permutations(paths)) {
                            String resolved = new TestingPathMatcher(ordering).matches(target);

                            if (first == null) {
                                first = String.valueOf(resolved);
                            } else if (!first.equals(String.valueOf(resolved))) {
                                unstable.add(paths + " on " + target + ": " + first + " vs " + resolved);
                                break;
                            }
                        }
                    }
                }
            }
        }

        Assertions.assertTrue(unstable.isEmpty(), unstable.size() + " order-dependent case(s), first few: "
                + unstable.subList(0, Math.min(5, unstable.size())));
    }

    /**
     * Asserts that the target resolves to the same expected path for every ordering of the configured paths. The
     * real callers build the matcher's path collection from a {@link HashMap}, so a rule that only happens to hold
     * for one iteration order is not a rule at all.
     */
    private static void assertResolvesTo(String expected, String target, String... paths) {
        for (List<String> ordering : permutations(Arrays.asList(paths))) {
            Assertions.assertEquals(expected, new TestingPathMatcher(ordering).matches(target),
                    "resolved differently for ordering " + ordering + " of target " + target);
        }
    }

    private static List<List<String>> permutations(List<String> values) {
        if (values.size() <= 1) {
            return Collections.singletonList(values);
        }

        List<List<String>> result = new ArrayList<>();

        for (int i = 0; i < values.size(); i++) {
            List<String> remaining = new ArrayList<>(values);
            String head = remaining.remove(i);

            for (List<String> tail : permutations(remaining)) {
                List<String> permutation = new ArrayList<>();
                permutation.add(head);
                permutation.addAll(tail);
                result.add(permutation);
            }
        }

        return result;
    }

    private static final class TestingPathMatcher extends PathMatcher<String> {
        private final Collection<String> paths;

        TestingPathMatcher(Collection<String> paths) {
            this.paths = paths;
        }

        @Override
        protected String getPath(String entry) {
            return entry;
        }

        @Override
        protected Collection<String> getPaths() {
            return paths;
        }
    }
}
