package org.keycloak.services.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:daniel.fesenmeyer@bosch.com">Daniel Fesenmeyer</a>
 */
public class LocaleUtilTest {

    private static final Locale LOCALE_DE_CH = Locale.forLanguageTag("de-CH");
    private static final Locale LOCALE_DE_CH_1996 = Locale.forLanguageTag("de-CH-1996");

    @Test
    public void getParentLocale() {
        assertThat(LocaleUtil.getParentLocale(LOCALE_DE_CH_1996), equalTo(LOCALE_DE_CH));
        assertThat(LocaleUtil.getParentLocale(LOCALE_DE_CH), equalTo(Locale.GERMAN));
        assertThat(LocaleUtil.getParentLocale(Locale.GERMAN), equalTo(Locale.ENGLISH));

        assertThat(LocaleUtil.getParentLocale(Locale.ENGLISH), nullValue());
    }

    @Test
    public void getApplicableLocales() {
        assertThat(LocaleUtil.getApplicableLocales(LOCALE_DE_CH_1996),
                equalTo(Arrays.asList(LOCALE_DE_CH_1996, LOCALE_DE_CH, Locale.GERMAN, Locale.ENGLISH)));
        assertThat(LocaleUtil.getApplicableLocales(LOCALE_DE_CH),
                equalTo(Arrays.asList(LOCALE_DE_CH, Locale.GERMAN, Locale.ENGLISH)));
        assertThat(LocaleUtil.getApplicableLocales(Locale.GERMAN),
                equalTo(Arrays.asList(Locale.GERMAN, Locale.ENGLISH)));

        assertThat(LocaleUtil.getApplicableLocales(Locale.ENGLISH), equalTo(Collections.singletonList(Locale.ENGLISH)));
    }

    @Test
    public void mergeGroupedMessages() {
        Map<Locale, Properties> groupedMessages = new HashMap<>();

        String keyDefinedEverywhere = "everywhere";
        String keyDefinedForRegionAndParents = "region-and-parents";
        String keyDefinedForLanguageAndParents = "language-and-parents";
        String keyDefinedForEnglishOnly = "english-only";

        // add messages for an irrelevant locale, in order to check that such messages are not in the merged result
        Properties irrelevantMessages = new Properties();
        addTestValue(irrelevantMessages, "french-only", Locale.FRENCH);
        groupedMessages.put(Locale.FRENCH, irrelevantMessages);

        Properties variantMessages = new Properties();
        addTestValue(variantMessages, keyDefinedEverywhere, LOCALE_DE_CH_1996);
        groupedMessages.put(LOCALE_DE_CH_1996, variantMessages);

        Properties regionMessages = new Properties();
        addTestValues(regionMessages, Arrays.asList(keyDefinedEverywhere, keyDefinedForRegionAndParents), LOCALE_DE_CH);
        groupedMessages.put(LOCALE_DE_CH, regionMessages);

        Properties languageMessages = new Properties();
        addTestValues(languageMessages, Arrays.asList(keyDefinedEverywhere, keyDefinedForRegionAndParents,
                keyDefinedForLanguageAndParents), Locale.GERMAN);
        groupedMessages.put(Locale.GERMAN, languageMessages);

        Properties englishMessages = new Properties();
        addTestValues(englishMessages, Arrays.asList(keyDefinedEverywhere, keyDefinedForRegionAndParents,
                keyDefinedForLanguageAndParents, keyDefinedForEnglishOnly), Locale.ENGLISH);
        groupedMessages.put(Locale.ENGLISH, englishMessages);

        Properties mergedMessages = LocaleUtil.mergeGroupedMessages(LOCALE_DE_CH_1996, groupedMessages);

        Properties expectedMergedMessages = new Properties();
        addTestValue(expectedMergedMessages, keyDefinedEverywhere, LOCALE_DE_CH_1996);
        addTestValue(expectedMergedMessages, keyDefinedForRegionAndParents, LOCALE_DE_CH);
        addTestValue(expectedMergedMessages, keyDefinedForLanguageAndParents, Locale.GERMAN);
        addTestValue(expectedMergedMessages, keyDefinedForEnglishOnly, Locale.ENGLISH);

        assertThat(mergedMessages, equalTo(expectedMergedMessages));
    }

    @Test
    public void mergeGroupedMessagesFromTwoSources() {
        // messages with priority 1
        Map<Locale, Properties> groupedMessages1 = new HashMap<>();
        // messages with priority 2
        Map<Locale, Properties> groupedMessages2 = new HashMap<>();

        String messages1Prefix = "msg1";
        String messages2Prefix = "msg2";

        String keyDefinedForVariantFromMessages1AndFallbacks = "variant1-and-fallbacks";
        String keyDefinedForVariantFromMessages2AndFallbacks = "variant2-and-fallbacks";
        String keyDefinedForRegionFromMessages1AndFallbacks = "region1-and-fallbacks";
        String keyDefinedForRegionFromMessages2AndFallbacks = "region2-and-fallbacks";
        String keyDefinedForLanguageFromMessages1AndFallbacks = "language1-and-fallbacks";
        String keyDefinedForLanguageFromMessages2AndFallbacks = "language2-and-fallbacks";
        String keyDefinedForEnglishFromMessages1AndFallback = "english1-and-fallback";
        String keyDefinedForEnglishFromMessages2only = "english2-only";

        // add messages for an irrelevant locale, in order to check that such messages are not in the merged result
        Properties irrelevantMessages = new Properties();
        addTestValue(irrelevantMessages, "french-only", Locale.FRENCH);
        groupedMessages1.put(Locale.FRENCH, irrelevantMessages);
        groupedMessages2.put(Locale.FRENCH, irrelevantMessages);

        Properties variant1Messages = new Properties();
        addTestValue(variant1Messages, keyDefinedForVariantFromMessages1AndFallbacks, LOCALE_DE_CH_1996,
                messages1Prefix);
        groupedMessages1.put(LOCALE_DE_CH_1996, variant1Messages);

        Properties variant2Messages = new Properties();
        addTestValues(variant2Messages, Arrays.asList(keyDefinedForVariantFromMessages1AndFallbacks,
                keyDefinedForVariantFromMessages2AndFallbacks), LOCALE_DE_CH_1996, messages2Prefix);
        groupedMessages2.put(LOCALE_DE_CH_1996, variant2Messages);

        Properties region1Messages = new Properties();
        addTestValues(region1Messages, Arrays.asList(keyDefinedForVariantFromMessages1AndFallbacks,
                keyDefinedForVariantFromMessages2AndFallbacks, keyDefinedForRegionFromMessages1AndFallbacks),
                LOCALE_DE_CH, messages1Prefix);
        groupedMessages1.put(LOCALE_DE_CH, region1Messages);

        Properties region2Messages = new Properties();
        addTestValues(region2Messages, Arrays.asList(keyDefinedForVariantFromMessages1AndFallbacks,
                keyDefinedForVariantFromMessages2AndFallbacks, keyDefinedForRegionFromMessages1AndFallbacks,
                keyDefinedForRegionFromMessages2AndFallbacks), LOCALE_DE_CH, messages2Prefix);
        groupedMessages2.put(LOCALE_DE_CH, region2Messages);

        Properties language1Messages = new Properties();
        addTestValues(language1Messages, Arrays.asList(keyDefinedForVariantFromMessages1AndFallbacks,
                keyDefinedForVariantFromMessages2AndFallbacks, keyDefinedForRegionFromMessages1AndFallbacks,
                keyDefinedForRegionFromMessages2AndFallbacks, keyDefinedForLanguageFromMessages1AndFallbacks),
                Locale.GERMAN, messages1Prefix);
        groupedMessages1.put(Locale.GERMAN, language1Messages);

        Properties language2Messages = new Properties();
        addTestValues(language2Messages, Arrays.asList(keyDefinedForVariantFromMessages1AndFallbacks,
                keyDefinedForVariantFromMessages2AndFallbacks, keyDefinedForRegionFromMessages1AndFallbacks,
                keyDefinedForRegionFromMessages2AndFallbacks, keyDefinedForLanguageFromMessages1AndFallbacks,
                keyDefinedForLanguageFromMessages2AndFallbacks), Locale.GERMAN, messages2Prefix);
        groupedMessages2.put(Locale.GERMAN, language2Messages);

        Properties english1Messages = new Properties();
        addTestValues(english1Messages, Arrays.asList(keyDefinedForVariantFromMessages1AndFallbacks,
                keyDefinedForVariantFromMessages2AndFallbacks, keyDefinedForRegionFromMessages1AndFallbacks,
                keyDefinedForRegionFromMessages2AndFallbacks, keyDefinedForLanguageFromMessages1AndFallbacks,
                keyDefinedForLanguageFromMessages2AndFallbacks, keyDefinedForEnglishFromMessages1AndFallback),
                Locale.ENGLISH, messages1Prefix);
        groupedMessages1.put(Locale.ENGLISH, english1Messages);

        Properties english2Messages = new Properties();
        addTestValues(english2Messages, Arrays.asList(keyDefinedForVariantFromMessages1AndFallbacks,
                keyDefinedForVariantFromMessages2AndFallbacks, keyDefinedForRegionFromMessages1AndFallbacks,
                keyDefinedForRegionFromMessages2AndFallbacks, keyDefinedForLanguageFromMessages1AndFallbacks,
                keyDefinedForLanguageFromMessages2AndFallbacks, keyDefinedForEnglishFromMessages1AndFallback,
                keyDefinedForEnglishFromMessages2only), Locale.ENGLISH, messages2Prefix);
        groupedMessages2.put(Locale.ENGLISH, english2Messages);

        Properties mergedMessages =
                LocaleUtil.mergeGroupedMessages(LOCALE_DE_CH_1996, groupedMessages1, groupedMessages2);

        Properties expectedMergedMessages = new Properties();
        addTestValue(expectedMergedMessages, keyDefinedForVariantFromMessages1AndFallbacks, LOCALE_DE_CH_1996,
                messages1Prefix);
        addTestValue(expectedMergedMessages, keyDefinedForVariantFromMessages2AndFallbacks, LOCALE_DE_CH_1996,
                messages2Prefix);
        addTestValue(expectedMergedMessages, keyDefinedForRegionFromMessages1AndFallbacks, LOCALE_DE_CH,
                messages1Prefix);
        addTestValue(expectedMergedMessages, keyDefinedForRegionFromMessages2AndFallbacks, LOCALE_DE_CH,
                messages2Prefix);
        addTestValue(expectedMergedMessages, keyDefinedForLanguageFromMessages1AndFallbacks, Locale.GERMAN,
                messages1Prefix);
        addTestValue(expectedMergedMessages, keyDefinedForLanguageFromMessages2AndFallbacks, Locale.GERMAN,
                messages2Prefix);
        addTestValue(expectedMergedMessages, keyDefinedForEnglishFromMessages1AndFallback, Locale.ENGLISH,
                messages1Prefix);
        addTestValue(expectedMergedMessages, keyDefinedForEnglishFromMessages2only, Locale.ENGLISH, messages2Prefix);

        assertThat(mergedMessages, equalTo(expectedMergedMessages));
    }

    private static void addTestValues(Properties messages, List<String> keys, Locale locale) {
        keys.forEach(k -> addTestValue(messages, k, locale));
    }

    private static void addTestValue(Properties messages, String key, Locale locale) {
        messages.put(key, createTestValue(key, locale, null));
    }

    private static void addTestValues(Properties messages, List<String> keys, Locale locale, String prefix) {
        keys.forEach(k -> addTestValue(messages, k, locale, prefix));
    }

    private static void addTestValue(Properties messages, String key, Locale locale, String prefix) {
        messages.put(key, createTestValue(key, locale, prefix));
    }

    private static String createTestValue(String key, Locale locale, String prefix) {
        return (prefix != null ? prefix + ":" : "") + locale.toLanguageTag() + ":" + key;
    }
}
