package org.keycloak.freemarke;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.freemarker.LocaleHelper;

import java.util.Arrays;
import java.util.HashSet;

/**
 * @author <a href="mailto:gerbermichi@me.com">Michael Gerber</a>
 */
public class LocaleHelperTest {
    @Test
    public void findLocaleTest(){
        Assert.assertEquals("de", LocaleHelper.findLocale("de", new HashSet<>(Arrays.asList("de","en"))).toLanguageTag());
        Assert.assertEquals("en", LocaleHelper.findLocale("en", new HashSet<>(Arrays.asList("de","en"))).toLanguageTag());
        Assert.assertEquals("de", LocaleHelper.findLocale("de-CH", new HashSet<>(Arrays.asList("de","en"))).toLanguageTag());
        Assert.assertEquals("de-CH", LocaleHelper.findLocale("de-CH", new HashSet<>(Arrays.asList("de","de-CH","de-DE"))).toLanguageTag());
        Assert.assertEquals("de-DE", LocaleHelper.findLocale("de-DE", new HashSet<>(Arrays.asList("de","de-CH","de-DE"))).toLanguageTag());
        Assert.assertEquals("de", LocaleHelper.findLocale("de", new HashSet<>(Arrays.asList("de","de-CH","de-DE"))).toLanguageTag());
        Assert.assertNull(LocaleHelper.findLocale("de", new HashSet<>(Arrays.asList("de-CH","de-DE"))));
    }
}
