package org.keycloak.locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class LocaleNegotiatorTest {

    private LocaleNegotiator localeNegotiator;

    @Before
    public void setUp() {
        Set<String> supportedLocales = new HashSet<>();
        supportedLocales.add("de");
        supportedLocales.add("de-AT");
        supportedLocales.add("de-CH");
        supportedLocales.add("de-DE");
        supportedLocales.add("pt-BR");
        localeNegotiator = new LocaleNegotiator(supportedLocales);
    }

    @Test
    public void shouldMatchWithoutCountryCode() {
        String expectedLocaleString = "de";
        LocaleSelection actualLocale = localeNegotiator.invoke(expectedLocaleString);
        Assert.assertEquals(Locale.GERMAN, actualLocale.getLocale());
        Assert.assertEquals(expectedLocaleString, actualLocale.getLocaleString());
    }

    @Test
    public void shouldMatchWithPriorityCountryCode() {
        String expectedLocaleString = "de-CH";
        LocaleSelection actualLocale = localeNegotiator.invoke(expectedLocaleString, "de");
        Assert.assertEquals(new Locale("de", "CH"), actualLocale.getLocale());
        Assert.assertEquals(expectedLocaleString, actualLocale.getLocaleString());
    }

    @Test
    public void shouldMatchWithPriorityNoCountryCode() {
        String expectedLocaleString = "de";
        LocaleSelection actualLocale = localeNegotiator.invoke(expectedLocaleString, "de-CH");
        Assert.assertEquals(new Locale(expectedLocaleString), actualLocale.getLocale());
        Assert.assertEquals(expectedLocaleString, actualLocale.getLocaleString());
    }

    @Test
    public void shouldMatchOmittedCountryCodeWithBestFit() {
        String expectedLocaleString = "pt";
        LocaleSelection actualLocale = localeNegotiator.invoke(expectedLocaleString, "es-ES");
        Assert.assertEquals(new Locale("pt", "BR"), actualLocale.getLocale());
        Assert.assertEquals(expectedLocaleString, actualLocale.getLocaleString());
    }
}
