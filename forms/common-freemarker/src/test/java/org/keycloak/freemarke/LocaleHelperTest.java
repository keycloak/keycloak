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
        Assert.assertEquals("de", LocaleHelper.findLocale(new HashSet<>(Arrays.asList("de","en")), "de").toLanguageTag());
        Assert.assertEquals("en", LocaleHelper.findLocale(new HashSet<>(Arrays.asList("de","en")), "en").toLanguageTag());
        Assert.assertEquals("de", LocaleHelper.findLocale(new HashSet<>(Arrays.asList("de","en")), "de-CH").toLanguageTag());
        Assert.assertEquals("de-CH", LocaleHelper.findLocale(new HashSet<>(Arrays.asList("de","de-CH","de-DE")), "de-CH").toLanguageTag());
        Assert.assertEquals("de-DE", LocaleHelper.findLocale(new HashSet<>(Arrays.asList("de","de-CH","de-DE")), "de-DE").toLanguageTag());
        Assert.assertEquals("de", LocaleHelper.findLocale(new HashSet<>(Arrays.asList("de","de-CH","de-DE")), "de").toLanguageTag());
        Assert.assertNull(LocaleHelper.findLocale(new HashSet<>(Arrays.asList("de-CH","de-DE")), "de"));
    }

    @Test
    public void findLocalesTest(){
        Assert.assertEquals("de", LocaleHelper.findLocale(new HashSet<>(Arrays.asList("de","en")), "de en".split(" ")).toLanguageTag());
        Assert.assertEquals("en", LocaleHelper.findLocale(new HashSet<>(Arrays.asList("de","en")), "en de".split(" ")).toLanguageTag());
        Assert.assertEquals("de", LocaleHelper.findLocale(new HashSet<>(Arrays.asList("de","en")), "de-CH en".split(" ")).toLanguageTag());
        Assert.assertEquals("de-CH", LocaleHelper.findLocale(new HashSet<>(Arrays.asList("de","de-CH","de-DE")), "de-CH de".split(" ")).toLanguageTag());
        Assert.assertEquals("de-DE", LocaleHelper.findLocale(new HashSet<>(Arrays.asList("de","de-CH","de-DE")), "de-DE de-CH de".split(" ")).toLanguageTag());
        Assert.assertEquals("de", LocaleHelper.findLocale(new HashSet<>(Arrays.asList("de","de-CH","de-DE")), "en fr de".split(" ")).toLanguageTag());
        Assert.assertNull(LocaleHelper.findLocale(new HashSet<>(Arrays.asList("de-CH","de-DE")), "de en fr".split(" ")));
    }
}
