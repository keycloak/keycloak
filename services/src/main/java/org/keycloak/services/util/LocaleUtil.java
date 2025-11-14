/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.keycloak.services.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.locale.LocaleUpdaterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:daniel.fesenmeyer@bosch.com">Daniel Fesenmeyer</a>
 */
public class LocaleUtil {

    private LocaleUtil() {
        // noop
    }

    public static void processLocaleParam(KeycloakSession session, RealmModel realm, AuthenticationSessionModel authSession) {
        if (realm.isInternationalizationEnabled()) {
            String locale = session.getContext().getUri().getQueryParameters().getFirst(LocaleSelectorProvider.KC_LOCALE_PARAM);
            if (locale != null) {
                if (authSession != null) {
                    authSession.setAuthNote(LocaleSelectorProvider.USER_REQUEST_LOCALE, locale);
                } else {
                    // Might be on info/error page when we don't have authenticationSession
                    session.setAttribute(LocaleSelectorProvider.USER_REQUEST_LOCALE, locale);
                }

                LocaleUpdaterProvider localeUpdater = session.getProvider(LocaleUpdaterProvider.class);
                localeUpdater.updateLocaleCookie(locale);
            }
        }
    }

    /**
     * Returns the parent locale of the given {@code locale}. If the locale just contains a language (e.g. "de"),
     * returns the fallback locale "en". For "en" no parent exists, {@code null} is returned.
     * 
     * @param locale the locale
     * @return the parent locale, may be {@code null}
     */
    public static Locale getParentLocale(Locale locale) {
        if (locale.getVariant() != null && !locale.getVariant().isEmpty()) {
            return new Locale(locale.getLanguage(), locale.getCountry());
        }

        if (locale.getCountry() != null && !locale.getCountry().isEmpty()) {
            return new Locale(locale.getLanguage());
        }

        if (!Locale.ENGLISH.equals(locale)) {
            return Locale.ENGLISH;
        }

        return null;
    }

    /**
     * Gets the applicable locales for the given locale.
     * <p>
     * Example: Locale "de-CH" has the applicable locales "de-CH", "de" and "en" (in exactly that order).
     * 
     * @param locale the locale
     * @return the applicable locales
     */
    static List<Locale> getApplicableLocales(Locale locale) {
        List<Locale> applicableLocales = new ArrayList<>();

        for (Locale currentLocale = locale; currentLocale != null; currentLocale = getParentLocale(currentLocale)) {
            applicableLocales.add(currentLocale);
        }

        return applicableLocales;
    }

    /**
     * Merge the given (locale-)grouped messages into one instance of {@link Properties}, applicable for the given
     * {@code locale}.
     * 
     * @param locale the locale
     * @param messages the (locale-)grouped messages
     * @return the merged properties
     * @see #mergeGroupedMessages(Locale, Map, Map)
     */
    public static Properties mergeGroupedMessages(Locale locale, Map<Locale, Properties> messages) {
        return mergeGroupedMessages(locale, messages, null);
    }

    /**
     * Merge the given (locale-)grouped messages into one instance of {@link Properties}, applicable for the given
     * {@code locale}.
     * <p>
     * The priority of the messages is as follows (abbreviations: F = firstMessages, S = secondMessages):
     * <ol>
     * <li>F &lt;language-region-variant&gt;</li>
     * <li>S &lt;language-region-variant&gt;</li>
     * <li>F &lt;language-region&gt;</li>
     * <li>S &lt;language-region&gt;</li>
     * <li>F &lt;language&gt;</li>
     * <li>S &lt;language&gt;</li>
     * <li>F en</li>
     * <li>S en</li>
     * </ol>
     * <p>
     * Example for the message priority for locale "de-CH-1996" (language "de", region "CH", variant "1996):
     * <ol>
     * <li>F de-CH-1996</li>
     * <li>S de-CH-1996</li>
     * <li>F de-CH</li>
     * <li>S de-CH</li>
     * <li>F de</li>
     * <li>S de</li>
     * <li>F en</li>
     * <li>S en</li>
     * </ol>
     * 
     * @param locale the locale
     * @param firstMessages the first (locale-)grouped messages, having higher priority (per locale) than
     *        {@code secondMessages}
     * @param secondMessages may be {@code null}, the second (locale-)grouped messages, having lower priority (per
     *        locale) than {@code firstMessages}
     * @return the merged properties
     * @see #mergeGroupedMessages(Locale, Map)
     */
    public static Properties mergeGroupedMessages(Locale locale, Map<Locale, Properties> firstMessages,
            Map<Locale, Properties> secondMessages) {
        List<Locale> applicableLocales = getApplicableLocales(locale);

        Properties mergedProperties = new Properties();

        /*
         * iterate starting from the end of the list in order to add the least relevant messages first (in order to be
         * overwritten by more relevant messages)
         */
        ListIterator<Locale> itr = applicableLocales.listIterator(applicableLocales.size());
        while (itr.hasPrevious()) {
            Locale currentLocale = itr.previous();

            // add secondMessages first, if specified (to be overwritten by firstMessages)
            if (secondMessages != null) {
                Properties currentLocaleSecondMessages = secondMessages.get(currentLocale);
                if (currentLocaleSecondMessages != null) {
                    mergedProperties.putAll(currentLocaleSecondMessages);
                }
            }

            // add firstMessages, overwriting secondMessages (if specified)
            Properties currentLocaleFirstMessages = firstMessages.get(currentLocale);
            if (currentLocaleFirstMessages != null) {
                mergedProperties.putAll(currentLocaleFirstMessages);
            }
        }

        return mergedProperties;
    }

    /**
     * Enhance the properties from a theme with realm localization texts. Realm localization texts take precedence over
     * the theme properties, but only when defined for the same locale. In general, texts for a more specific locale
     * take precedence over texts for a less specific locale.
     * <p>
     * For implementation details, see {@link #mergeGroupedMessages(Locale, Map, Map)}.
     * 
     * @param realm the realm from which the localization texts should be used
     * @param locale the locale for which the relevant texts should be retrieved
     * @param themeMessages the theme messages, which should be enhanced and maybe overwritten
     * @return the enhanced properties
     */
    public static Properties enhancePropertiesWithRealmLocalizationTexts(RealmModel realm, Locale locale,
            Map<Locale, Properties> themeMessages) {
        Map<Locale, Properties> realmLocalizationMessages = getRealmLocalizationTexts(realm, locale);

        return mergeGroupedMessages(locale, realmLocalizationMessages, themeMessages);
    }

    public static Map<Locale, Properties> getRealmLocalizationTexts(RealmModel realm, Locale locale) {
        LinkedHashMap<Locale, Properties> groupedMessages = new LinkedHashMap<>();

        List<Locale> applicableLocales = getApplicableLocales(locale);
        for (Locale applicableLocale : applicableLocales) {
            Map<String, String> currentRealmLocalizationTexts =
                    realm.getRealmLocalizationTextsByLocale(applicableLocale.toLanguageTag());
            Properties currentMessages = new Properties();
            currentMessages.putAll(currentRealmLocalizationTexts);

            groupedMessages.put(applicableLocale, currentMessages);
        }

        return groupedMessages;
    }
    
}
