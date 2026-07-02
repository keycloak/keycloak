package org.keycloak.common.util;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PathMatcherTest {

    @Test
    public void keycloak15833Test() {
        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList(
                "/api/v1/{clientId}/campaigns/*"
        ));

        Assertions.assertNotNull(matcher.matches("/api/v1/1/campaigns/summer"));
        Assertions.assertNull(matcher.matches("/api/v1/1/contentConnectorConfigs/29/contentConnectorContents"));
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
    public void emptyPlaceholderShouldNotMatch() {
        TestingPathMatcher matcher = new TestingPathMatcher(Collections.singletonList(
                "/api/{}/foo"
        ));

        Assertions.assertNull(matcher.matches("/api/1/foo"));
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
