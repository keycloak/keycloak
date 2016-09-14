package org.keycloak.protocol.oidc.utils;

import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class RegexifyTest {

    @Test
    public void testToRegexString_null() {
        assertThat(Regexify.asString.apply(null), equalTo(Regexify.MATCH_NOTHING_REGEX));
    }

    @Test
    public void testToRegexString_empty() {
        assertThat(Regexify.asString.apply(""), equalTo("^$"));
    }

    @Test
    public void testToRegexString_basic() {
        assertThat(Regexify.asString.apply("http://"), equalTo("^http://$"));
    }

    @Test
    public void testToRegexString_wildcardStart() {
        assertThat(Regexify.asString.apply("*tp://"), equalTo("^.*tp://$"));
    }

    @Test
    public void testToRegexString_wildcardEnd() {
        assertThat(Regexify.asString.apply("http*://"), equalTo("^http.*://$"));
    }

    @Test
    public void testToRegexString_escapeMetachars() {
        assertThat(Regexify.asString.apply("with-dash.example.com"), equalTo("^with\\-dash\\.example\\.com$"));
    }

    @Test
    public void testToRegexString_escapeMetacharsWithWidlcard() {
        assertThat(Regexify.asString.apply("dev.*.example.com"), equalTo("^dev\\..*\\.example\\.com$"));
    }

}
