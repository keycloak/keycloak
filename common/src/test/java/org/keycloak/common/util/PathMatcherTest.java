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
