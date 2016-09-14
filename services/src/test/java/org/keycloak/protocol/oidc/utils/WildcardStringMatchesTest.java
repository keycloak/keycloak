package org.keycloak.protocol.oidc.utils;

import org.junit.Test;
import org.keycloak.protocol.oidc.utils.url.WildcardStringMatches;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class WildcardStringMatchesTest {

    @Test
    public void shouldMatch_whenSimpleUrl() {
        assertThat(new WildcardStringMatches("http://www.example.com").test("http://www.example.com"), equalTo(true));
    }

    @Test
    public void shouldMatch_whenQueryParamsAndRefs() {
        assertThat(new WildcardStringMatches("http://www.example.com/path/to/page.html?lang=em#section5")
                .test("http://www.example.com/path/to/page.html?lang=em#section5"), equalTo(true));
    }

    @Test
    public void shouldNot_whenAttemptWildcardProtocol() {
        assertThat(new WildcardStringMatches("http://www.example.com").test("*://www.example.com"), equalTo(false));
    }

    @Test
    public void shouldMatch_whenWildcardSubdomain() {
        assertThat(new WildcardStringMatches("http://*.com").test("http://www.example.com"), equalTo(true));
    }

    @Test
    public void shouldMatch_whenWildcardMiddleOfSubdomain() {
        assertThat(new WildcardStringMatches("http://group.*.example.com").test("http://group.sales.example.com"), equalTo(true));
    }

    @Test
    public void shouldMatch_whenSeveralWildcardSubdomain() {
        assertThat(new WildcardStringMatches("http://group.*.region.*.example.com").test("http://group.sales.region.us.example.com"), equalTo(true));
    }

    @Test
    public void shouldNot_whenWildcardNotMatch() {
        assertThat(new WildcardStringMatches("http://group.*.example.com").test("http://sales.example.com"), equalTo(false));
    }

    @Test
    public void shouldMatch_withPort() {
        assertThat(new WildcardStringMatches("http://www.example.com:80").test("http://www.example.com:80"), equalTo(true));
    }

    @Test
    public void shouldNot_wildcardPort() {
        assertThat(new WildcardStringMatches("http://www.example.com:*").test("http://www.example.com:443"), equalTo(false));
    }

    @Test
    public void shouldNot_portMismatch() {
        assertThat(new WildcardStringMatches("http://www.example.com:443").test("http://www.example.com"), equalTo(false));
    }

    @Test
    public void shouldNot_portWillNotDefaultTo80() {
        assertThat(new WildcardStringMatches("http://www.example.com").test("http://www.example.com:80"), equalTo(false));
    }

    @Test
    public void shouldNot_portUnspecified() {
        assertThat(new WildcardStringMatches("http://www.example.com:443").test("http://www.example.com"), equalTo(false));
    }

    @Test
    public void shouldMatch_withPath() {
        assertThat(new WildcardStringMatches("http://www.example.com/path/to/file.html")
                .test("http://www.example.com/path/to/file.html"), equalTo(true));
    }

    @Test
    public void shouldMatch_wildcardPath() {
        assertThat(new WildcardStringMatches("http://www.example.com/*")
                .test("http://www.example.com/path/to/file.html"), equalTo(true));
    }

    @Test
    public void shouldMatch_wildcardQueryParams() {
        assertThat(new WildcardStringMatches("http://www.example.com/*")
                .test("http://www.example.com/path/to/file.html?lang=en"), equalTo(true));
    }

    @Test
    public void shouldMatch_wildcardSubPath() {
        assertThat(new WildcardStringMatches("http://www.example.com/*/file.html")
                .test("http://www.example.com/path/to/file.html"), equalTo(true));
    }

    @Test
    public void shouldNot_pathsDiffer() {
        assertThat(new WildcardStringMatches("http://www.example.com/path/landing.html")
                .test("http://www.example.com/path/to/file.html"), equalTo(false));
    }

    @Test
    public void shouldNot_wildcardPathDiffers() {
        assertThat(new WildcardStringMatches("http://www.example.com/*landing.html")
                .test("http://www.example.com/path/to/file.html"), equalTo(false));
    }

    @Test
    public void shouldMatch_trailingSlashStarPath() {
        assertThat(new WildcardStringMatches("http://localhost:8180/foo/*")
                .test("http://localhost:8180/foo"), equalTo(true));
    }

    @Test
    public void shouldMatch_trailingSlashStar() {
        assertThat(new WildcardStringMatches("http://www.example.com/*")
                .test("http://www.example.com"), equalTo(true));
    }

    @Test
    public void shouldNot_pathWithTrailingSlash() {
        assertThat(new WildcardStringMatches("http://www.example.com/splash/")
                .test("http://www.example.com/splash"), equalTo(false));
    }
}
