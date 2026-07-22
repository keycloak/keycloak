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
    public void testPercentEncodedSlashDecoded() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertEquals("/api/admin", matcher.matches("/api/%2Fadmin"));
    }

    @Test
    public void testCombinedVectors() {
        PathMatcher<String> matcher = createMatcher("/api/admin", "/*");
        Assert.assertEquals("/api/admin", matcher.matches("/api/admin;x=1/"));
        Assert.assertEquals("/api/admin", matcher.matches("//api/foo/../admin;y=2/"));
        Assert.assertEquals("/api/admin", matcher.matches("/api/./admin;session=abc"));
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
