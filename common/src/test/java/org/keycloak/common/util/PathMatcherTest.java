package org.keycloak.common.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class PathMatcherTest {

    @Test
    public void keycloak15833Test() {
        TestingPathMatcher matcher = new TestingPathMatcher();

        Assert.assertEquals("/api/v1/1/campaigns/*/excelFiles", matcher.customBuildUriFromTemplate("/api/v1/{clientId}/campaigns/*/excelFiles", "/api/v1/1/contentConnectorConfigs/29/contentConnectorContents", false));
    }

    @Test
    public void testMatrixParamsStripped() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertEquals("/api/admin", matcher.matches("/api/admin;x=1"));
        Assert.assertEquals("/api/admin", matcher.matches("/api/admin;jsessionid=abc123"));
        Assert.assertEquals("/api/admin", matcher.matches("/api/admin;a=1;b=2"));
    }

    @Test
    public void testMatrixParamsInMiddleSegment() {
        PathMatcher<String> matcher = createMatcher("/api/admin/data", "/*");
        Assert.assertEquals("/api/admin/data", matcher.matches("/api;v=2/admin/data"));
        Assert.assertEquals("/api/admin/data", matcher.matches("/api/admin;x=1/data"));
    }

    @Test
    public void testTrailingSlashStripped() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertEquals("/api/admin", matcher.matches("/api/admin/"));
    }

    @Test
    public void testDoubleSlashesCollapsed() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertEquals("/api/admin", matcher.matches("/api//admin"));
        Assert.assertEquals("/api/admin", matcher.matches("//api///admin"));
    }

    @Test
    public void testDotSegmentsResolved() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertEquals("/api/admin", matcher.matches("/api/foo/../admin"));
        Assert.assertEquals("/api/admin", matcher.matches("/api/./admin"));
    }

    @Test
    public void testPercentEncodedUnreservedDecoded() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertEquals("/api/admin", matcher.matches("/api/%61dmin"));
        Assert.assertEquals("/api/admin", matcher.matches("/%61pi/%61dmin"));
    }

    @Test
    public void testPercentEncodedReservedDecoded() {
        PathMatcher<String> matcher = createMatcher("/api/admin area", "/*");
        Assert.assertEquals("/api/admin area", matcher.matches("/api/admin%20area"));
    }

    @Test
    public void testPercentEncodedSemicolonStripped() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertEquals("/api/admin", matcher.matches("/api/admin%3Bx=1"));
        Assert.assertEquals("/api/admin", matcher.matches("/api/admin%3bx=1"));
    }

    @Test
    public void testPercentEncodedSemicolonInMiddleSegment() {
        PathMatcher<String> matcher = createMatcher("/api/admin/data", "/*");
        Assert.assertEquals("/api/admin/data", matcher.matches("/api/admin%3Bx=1/data"));
    }

    @Test
    public void testPercentEncodedSlashDecoded() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertEquals("/api/admin", matcher.matches("/api/%2Fadmin"));
    }

    @Test
    public void testPercentEncodedDotSegmentsResolved() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertEquals("/api/admin", matcher.matches("/api/foo%2F..%2Fadmin"));
        Assert.assertEquals("/api/admin", matcher.matches("/api/foo/%2E%2E/admin"));
        Assert.assertEquals("/api/admin", matcher.matches("/api/foo%2F..%2F.%2Fadmin"));
        Assert.assertEquals("/api/admin", matcher.matches("/api/x%2F..%2Fy%2F..%2Fadmin"));
        Assert.assertEquals("/api/admin", matcher.matches("/api/x/%2e%2e/admin"));
        Assert.assertEquals("/api/admin", matcher.matches("/api/x/%2e./admin"));
        Assert.assertEquals("/api/admin", matcher.matches("/api/x/.%2e/admin"));
    }

    @Test
    public void testPercentEncodedDotCurrentDirResolved() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertEquals("/api/admin", matcher.matches("/api/%2E/admin"));
        Assert.assertEquals("/api/admin", matcher.matches("/api/.%2Fadmin"));
    }

    @Test
    public void testPercentEncodedTrailingSlashStripped() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertEquals("/api/admin", matcher.matches("/api/admin%2F"));
    }

    @Test
    public void testPercentEncodedDoubleSlashCollapsed() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertEquals("/api/admin", matcher.matches("/api%2F%2Fadmin"));
        Assert.assertEquals("/api/admin", matcher.matches("/api%2Fadmin"));
    }

    @Test
    public void testDoubleEncodedSlashNotFurtherDecoded() {
        // a double-encoded slash ("%252F") decodes exactly once, to the literal text "%2F" - it must not be
        // decoded a second time into a real path separator. Servlet containers only ever decode one layer, so
        // decoding further here would mismatch what the container actually routes to.
        PathMatcher<String> matcher = createMatcher("/api/%2Fadmin", "/api/admin", "/*");
        Assert.assertEquals("/api/%2Fadmin", matcher.matches("/api/%252Fadmin"));
    }

    @Test
    public void testDoubleEncodedSlashDoesNotMatchSingleDecodedResource() {
        // consequently, a resource configured with the plain decoded path must NOT match a double-encoded
        // slash - it falls through to the wildcard instead, it does not resolve to "/api/admin"
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertEquals("/*", matcher.matches("/api/%252Fadmin"));
    }

    @Test
    public void testDoubleEncodedSlashDoesNotProduceUnresolvedDoubleSlash() {
        // the second (post-decode) normalize() pass uses the 5-arg URI(scheme, authority, path, query, fragment)
        // constructor, which always re-quotes a literal '%' when building the raw path, so getPath() can never
        // re-decode text that already went through one decode pass - this must never contain "//"
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        String result = matcher.matches("/api/%252Fadmin");
        Assert.assertNotNull(result);
        Assert.assertFalse("Unexpected unresolved double slash in: " + result, result.contains("//"));
    }

    @Test
    public void testDoubleEncodedDotSegmentNotFurtherDecoded() {
        // same reasoning as above, for a double-encoded dot segment ("%252E%252E" decodes once to the literal
        // text "%2E%2E", not to ".."), so it must not be resolved as a dot segment by the second normalize() pass
        PathMatcher<String> matcher = createMatcher("/api/foo/%2E%2E/admin", "/api/admin", "/*");
        Assert.assertEquals("/api/foo/%2E%2E/admin", matcher.matches("/api/foo/%252E%252E/admin"));
    }

    @Test
    public void testCombinedVectors() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertEquals("/api/admin", matcher.matches("/api/admin;x=1/"));
        Assert.assertEquals("/api/admin", matcher.matches("//api/foo/../admin;y=2/"));
        Assert.assertEquals("/api/admin", matcher.matches("/api/./admin;session=abc"));
    }

    @Test
    public void testAbsoluteUriSchemeAndAuthorityPreserved() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/example", "/*");
        Assert.assertEquals("https://my.domain/example", matcher.matches("https://my.domain/example"));
    }

    @Test
    public void testAbsoluteUriWithPortPreserved() {
        PathMatcher<String> matcher = createMatcher("https://my.domain:8080/example", "/*");
        Assert.assertEquals("https://my.domain:8080/example", matcher.matches("https://my.domain:8080/example"));
    }

    @Test
    public void testAbsoluteUriMatchesTemplate() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/example/{module-name}", "/*");
        Assert.assertEquals("https://my.domain/example/{module-name}", matcher.matches("https://my.domain/example/one"));
    }

    @Test
    public void testAbsoluteUriMatchesWildcard() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/example/*", "/other");
        Assert.assertEquals("https://my.domain/example/*", matcher.matches("https://my.domain/example/one"));
    }

    @Test
    public void testAbsoluteUriMatrixParamsStripped() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/api/admin", "/*");
        Assert.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api/admin;x=1"));
    }

    @Test
    public void testAbsoluteUriPercentEncodedSemicolonStripped() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/api/admin", "/*");
        Assert.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api/admin%3Bx=1"));
        Assert.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api/admin%3bx=1"));
    }

    @Test
    public void testAbsoluteUriDotSegmentsResolved() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/api/admin", "/*");
        Assert.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api/foo/../admin"));
    }

    @Test
    public void testAbsoluteUriPercentEncodedDotSegmentsResolved() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/api/admin", "/*");
        Assert.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api/foo/%2E%2E/admin"));
        Assert.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api/foo%2F..%2Fadmin"));
    }

    @Test
    public void testAbsoluteUriDoubleSlashesInPathCollapsed() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/api/admin", "/*");
        Assert.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api//admin"));
    }

    @Test
    public void testAbsoluteUriTrailingSlashStripped() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/api/admin", "/*");
        Assert.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api/admin/"));
    }

    @Test
    public void testQueryStringDropped() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertEquals("/api/admin", matcher.matches("/api/admin?x=1"));
    }

    @Test
    public void testFragmentDropped() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertEquals("/api/admin", matcher.matches("/api/admin#section"));
    }

    @Test
    public void testQueryStringNotSplicedAcrossMatrixParam() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertEquals("/api/admin", matcher.matches("/api/admin;x=1?redirect=/evil"));
    }

    @Test
    public void testAbsoluteUriQueryStringDropped() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/api/admin", "/*");
        Assert.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api/admin?x=1"));
    }

    @Test
    public void testQueryWithNestedSchemeAndSlashesDropped() {
        // a query value containing its own "://" and "//" must not leak into, or be confused with, the kept prefix
        PathMatcher<String> matcher = createMatcher("https://host/path", "/*");
        Assert.assertEquals("https://host/path", matcher.matches("https://host/path?x=https://a//b"));
    }

    @Test
    public void testMalformedQueryDoesNotCorruptSchemeAndAuthority() {
        // a malformed %-escape in the (discarded) query must not poison the scheme/authority syntax probe and
        // fall through to mangling "https://" as if it were plain path content
        PathMatcher<String> matcher = createMatcher("https://my.domain/api/admin", "/*");
        Assert.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api/admin?bad=%"));
    }

    @Test
    public void testEncodedQuestionMarkNotTreatedAsDelimiter() {
        // %3F is not a literal '?', so it must not be truncated as a query delimiter - it survives as part of
        // the path and gets decoded normally by the existing single-decode pass, same as any other %-octet
        PathMatcher<String> matcher = createMatcher("/api/search?foo", "/*");
        Assert.assertEquals("/api/search?foo", matcher.matches("/api/search%3Ffoo"));
    }

    @Test
    public void testRootPathPreserved() {
        PathMatcher<String> matcher = createMatcher("/", "/api/admin");
        Assert.assertEquals("/", matcher.matches("/"));
    }

    @Test
    public void testNormalUriUnchanged() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertEquals("/api/admin", matcher.matches("/api/admin"));
    }

    @Test
    public void testWildcardMatchingStillWorks() {
        PathMatcher<String> matcher = createMatcher("/api/admin/*", "/other");
        Assert.assertEquals("/api/admin/*", matcher.matches("/api/admin/sub"));
        Assert.assertEquals("/api/admin/*", matcher.matches("/api/admin/sub/deep"));
    }

    @Test
    public void testTemplateMatchingStillWorks() {
        PathMatcher<String> matcher = createMatcher("/api/{id}/items", "/*");
        Assert.assertEquals("/api/{id}/items", matcher.matches("/api/123/items"));
    }

    @Test
    public void testCurlyBracesInTargetUri() {
        PathMatcher<String> matcher = createMatcher("/rest/{version}/carts/{cartId}/cartactions/{actionId}", "/*");
        Assert.assertNotNull(matcher.matches("/rest/v2/carts/{cartId}/cartactions/123"));
    }

    @Test
    public void testMalformedUriReturnsNoMatch() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertNull(matcher.matches("/api/foo/../admin%"));
        Assert.assertNull(matcher.matches("/api/admin%"));
    }

    @Test
    public void testUriWithNoPathReturnsNoMatch() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertNull(matcher.matches("foo:bar"));
    }

    @Test
    public void testMalformedAbsoluteUriReturnsNoMatch() {
        // a string that looks like an absolute URI (contains "://") but fails to parse as one must be rejected
        // outright, not fall back to treating the raw "scheme://" text as a plain path
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertNull(matcher.matches("https://[not-ipv6]/api/admin"));
    }

    @Test
    public void testAuthorityLessAbsoluteUriReturnsNoMatch() {
        // "https:///api/admin" has a recognized scheme but the empty authority comes back as null (not "") -
        // must be rejected outright, not fall back to treating "https://" as plain path content
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertNull(matcher.matches("https:///api/admin"));
    }

    @Test
    public void testIncidentalSchemeSeparatorInRelativePathStillNormalized() {
        // "://" embedded in an ordinary relative path (no recognized scheme, since it doesn't start with one) is
        // not an absolute URI at all - it must still fall through to plain path normalization, not be rejected
        PathMatcher<String> matcher = createMatcher("/api/redirect-to-https:/example.com", "/other");
        Assert.assertEquals("/api/redirect-to-https:/example.com", matcher.matches("/api/redirect-to-https://example.com"));
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
            Assert.assertNotNull("Should match for: " + mutated, result);
            Assert.assertEquals("Should match /api/admin, not /* for: " + mutated,
                    "/api/admin", result);
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

    private static final class TestingPathMatcher extends PathMatcher<Object> {

        @Override
        protected String getPath(Object entry) {
            return null;
        }

        @Override
        protected Collection<Object> getPaths() {
            return null;
        }

        // Make buildUriFromTemplate accessible from test
        public String customBuildUriFromTemplate(String template, String targetUri, boolean onlyFirstParam) {
            return buildUriFromTemplate(template, targetUri, onlyFirstParam);
        }
    }
}
