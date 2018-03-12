package org.keycloak.services.util;

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
        Locale actualLocale = localeNegotiator.invoke("de");
        Assert.assertEquals(Locale.GERMAN, actualLocale);
    }

    @Test
    public void shouldMatchWithPriorityCountryCode() {
        Locale actualLocale = localeNegotiator.invoke("de-CH", "de");
        Assert.assertEquals(new Locale("de", "CH"), actualLocale);
    }

    @Test
    public void shouldMatchWithPriorityNoCountryCode() {
        Locale actualLocale = localeNegotiator.invoke("de", "de-CH");
        Assert.assertEquals(new Locale("de"), actualLocale);
    }

    @Test
    public void shouldMatchOmittedCountryCodeWithBestFit() {
        Locale actualLocale = localeNegotiator.invoke("pt", "es-ES");
        Assert.assertEquals(new Locale("pt", "BR"), actualLocale);
    }
}
