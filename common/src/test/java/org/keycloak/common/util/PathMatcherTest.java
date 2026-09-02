package org.keycloak.common.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
    public void testPercentEncodedSlashDecoded() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assertions.assertEquals("/api/admin", matcher.matches("/api/%2Fadmin"));
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
    public void testAbsoluteUriDotSegmentsResolved() {
        PathMatcher<String> matcher = createMatcher("https://my.domain/api/admin", "/*");
        Assertions.assertEquals("https://my.domain/api/admin", matcher.matches("https://my.domain/api/foo/../admin"));
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
