package org.keycloak.utils;

import java.util.Arrays;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * <a href="mailto:external.benjamin.weimer@bosch-si.com">Benjamin Weimer</a>,
 */
public class RegexUtilsTest {
    @Test
    public void isValidRegexTest() {
        assertThat(RegexUtils.isValidRegex("myScope:(.*)"), is(true));
        assertThat(RegexUtils.isValidRegex("[0-9]+"), is(true));
        assertThat(RegexUtils.isValidRegex("true|false"), is(true));
        assertThat(RegexUtils.isValidRegex("simple"), is(true));
        assertThat(RegexUtils.isValidRegex("[invalid"), is(false));
        assertThat(RegexUtils.isValidRegex("(unclosed"), is(false));
        assertThat(RegexUtils.isValidRegex("*invalid"), is(false));
    }

    @Test
    public void isValidRegexWithConstraintsTest() {
        assertThat(RegexUtils.isValidRegex("[a-z]+", 512, false), is(true));
        assertThat(RegexUtils.isValidRegex("true|false", 512, false), is(true));
        assertThat(RegexUtils.isValidRegex("[0-9]{3,5}", 512, false), is(true));

        assertThat(RegexUtils.isValidRegex("(.*)", 512, false), is(false));
        assertThat(RegexUtils.isValidRegex("prefix(group)", 512, false), is(false));
        assertThat(RegexUtils.isValidRegex("(a)(b)", 512, true), is(true));

        assertThat(RegexUtils.isValidRegex("abc", 2, false), is(false));
        assertThat(RegexUtils.isValidRegex("ab", 2, false), is(true));

        assertThat(RegexUtils.isValidRegex(null, 512, false), is(false));
        assertThat(RegexUtils.isValidRegex("[invalid", 512, false), is(false));
    }

    @Test
    public void valueMatchesRegexTest() {
        assertThat(RegexUtils.valueMatchesRegex("AB.*", "AB_ADMIN"), is(true));
        assertThat(RegexUtils.valueMatchesRegex("AB.*", "AA_ADMIN"), is(false));
        assertThat(RegexUtils.valueMatchesRegex("99.*", 999), is(true));
        assertThat(RegexUtils.valueMatchesRegex("98.*", 999), is(false));
        assertThat(RegexUtils.valueMatchesRegex("99\\..*", 99.9), is(true));
        assertThat(RegexUtils.valueMatchesRegex("AB.*", null), is(false));
        assertThat(RegexUtils.valueMatchesRegex("AB.*", Arrays.asList("AB_ADMIN", "AA_ADMIN")), is(true));
    }

}
