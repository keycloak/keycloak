package org.keycloak.protocol.oidc.utils;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class WildcardUrlStringMatchesTest {

    @Test
    public void testToRegexString_basic() {
        assertThat(WildcardUrlStringMatches.toRegex("http://"), equalTo("^http://$"));
    }

    @Test
    public void testToRegexString_wildcardStart() {
        assertThat(WildcardUrlStringMatches.toRegex("*tp://"), equalTo("^.*tp://$"));
    }

    @Test
    public void testToRegexString_wildcardEnd() {
        assertThat(WildcardUrlStringMatches.toRegex("http*://"), equalTo("^http.*://$"));
    }

    @Test
    public void testToRegexString_escapeMetachars() {
        assertThat(WildcardUrlStringMatches.toRegex("with-dash.example.com"), equalTo("^with\\-dash\\.example\\.com$"));
    }

    @Test
    public void testToRegexString_escapeMetacharsWithWidlcard() {
        assertThat(WildcardUrlStringMatches.toRegex("dev.*.example.com"), equalTo("^dev\\..*\\.example\\.com$"));
    }

    @Test
    public void testWildcardMatch_base_url() {
        assertThat(new WildcardUrlStringMatches("http://www.example.com").test("http://www.example.com"), equalTo(true));
    }

    @Test
    public void testWildcardMatch_base_queryParamsAndRefs() {
        assertThat(new WildcardUrlStringMatches("http://www.example.com/path/to/page.html?lang=em#section5")
                .test("http://www.example.com/path/to/page.html?lang=em#section5"), equalTo(true));
    }

    @Test
    public void testWildcardMatch_protocol_wildcard() {
        assertThat(new WildcardUrlStringMatches("*://www.example.com").test("http://www.example.com"), equalTo(true));
    }

    @Test
    public void testWildcardMatch_protocol_wildcardPartial() {
        assertThat(new WildcardUrlStringMatches("http*://www.example.com").test("https://www.example.com"), equalTo(true));
    }

    @Test
    public void testWildcardMatch_protocol_wildcardWrongParam() {
        assertThat(new WildcardUrlStringMatches("http://www.example.com").test("*://www.example.com"), equalTo(false));
    }

    @Test
    public void testWildcardMatch_hostname_wildcardStart() {
        assertThat(new WildcardUrlStringMatches("http://*.com").test("http://www.example.com"), equalTo(true));
    }

    @Test
    public void testWildcardMatch_hostname_wildcardMiddle() {
        assertThat(new WildcardUrlStringMatches("http://group.*.example.com").test("http://group.sales.example.com"), equalTo(true));
    }

    @Test
    public void testWildcardMatch_hostname_wildcardDouble() {
        assertThat(new WildcardUrlStringMatches("http://group.*.region.*.example.com").test("http://group.sales.region.us.example.com"), equalTo(true));
    }

    @Test
    public void testWildcardMatch_hostname_wildcardInvalid() {
        assertThat(new WildcardUrlStringMatches("http://group.*.example.com").test("http://sales.example.com"), equalTo(false));
    }

    @Test
    public void testWildcardMatch_port_match() {
        assertThat(new WildcardUrlStringMatches("http://www.example.com:80").test("http://www.example.com:80"), equalTo(true));
    }

    @Test
    public void testWildcardMatch_port_wildcard() {
        assertThat(new WildcardUrlStringMatches("http://www.example.com:*").test("http://www.example.com:8080"), equalTo(true));
    }

    @Test
    public void testWildcardMatch_port_startWildcard() {
        assertThat(new WildcardUrlStringMatches("http://www.example.com:*443").test("http://www.example.com:8443"), equalTo(true));
    }

    @Test
    public void testWildcardMatch_port_startWildcardMismatch() {
        assertThat(new WildcardUrlStringMatches("http://www.example.com:*443").test("http://www.example.com:80"), equalTo(false));
    }

    @Test
    public void testWildcardMatch_port_mismatch() {
        assertThat(new WildcardUrlStringMatches("http://www.example.com:443").test("http://www.example.com"), equalTo(false));
    }

    @Test
    public void testWildcardMatch_port_unspecifiedMismatch() {
        assertThat(new WildcardUrlStringMatches("http://www.example.com").test("http://www.example.com:80"), equalTo(false));
    }

    @Test
    public void testWildcardMatch_port_unspecifiedWhenWildcardRequired() {
        assertThat(new WildcardUrlStringMatches("http://www.example.com:*443").test("http://www.example.com"), equalTo(false));
    }

    @Test
    public void testWildcardMatch_path_matching() {
        assertThat(new WildcardUrlStringMatches("http://www.example.com/path/to/file.html")
                .test("http://www.example.com/path/to/file.html"), equalTo(true));
    }

    @Test
    public void testWildcardMatch_path_wildcard() {
        assertThat(new WildcardUrlStringMatches("http://www.example.com/*")
                .test("http://www.example.com/path/to/file.html"), equalTo(true));
    }

    @Test
    public void testWildcardMatch_path_wildcardWithQueryParams() {
        assertThat(new WildcardUrlStringMatches("http://www.example.com/*")
                .test("http://www.example.com/path/to/file.html?lang=en"), equalTo(true));
    }

    @Test
    public void testWildcardMatch_path_subpathWildcard() {
        assertThat(new WildcardUrlStringMatches("http://www.example.com/*/file.html")
                .test("http://www.example.com/path/to/file.html"), equalTo(true));
    }

    @Test
    public void testWildcardMatch_path_mismatch() {
        assertThat(new WildcardUrlStringMatches("http://www.example.com/path/landing.html")
                .test("http://www.example.com/path/to/file.html"), equalTo(false));
    }

    @Test
    public void testWildcardMatch_path_wildcardMismatch() {
        assertThat(new WildcardUrlStringMatches("http://www.example.com/*landing.html")
                .test("http://www.example.com/path/to/file.html"), equalTo(false));
    }

    @Test
    public void testWildcardMatch_path_matchWithTrailingPathSlashWildcard() {
        assertThat(new WildcardUrlStringMatches("http://localhost:8180/foo/*")
                .test("http://localhost:8180/foo"), equalTo(true));
    }

    @Test
    public void testWildcardMatch_path_matchWithTrailingHostSlashWildcard() {
        assertThat(new WildcardUrlStringMatches("http://www.example.com/*")
                .test("http://www.example.com"), equalTo(true));
    }

    @Test
    public void testWildcardMatch_path_noMatchWithTrailingSlash() {
        assertThat(new WildcardUrlStringMatches("http://www.example.com/splash/")
                .test("http://www.example.com/splash"), equalTo(false));
    }
}
