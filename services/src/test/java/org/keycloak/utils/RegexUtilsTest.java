package org.keycloak.utils;

import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * <a href="mailto:external.benjamin.weimer@bosch-si.com">Benjamin Weimer</a>,
 */
public class RegexUtilsTest {
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
