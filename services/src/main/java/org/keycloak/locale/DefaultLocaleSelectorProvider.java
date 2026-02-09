/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
 */
package org.keycloak.locale;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.cookie.CookieProvider;
import org.keycloak.cookie.CookieType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.jboss.logging.Logger;

public class DefaultLocaleSelectorProvider implements LocaleSelectorProvider {

    private static final Logger logger = Logger.getLogger(LocaleSelectorProvider.class);

    private KeycloakSession session;

    public DefaultLocaleSelectorProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Locale resolveLocale(RealmModel realm, UserModel user) {
        return resolveLocale(realm, user, false);
    }

    @Override
    public Locale resolveLocale(RealmModel realm, UserModel user, boolean ignoreAcceptLanguageHeader) {
        HttpHeaders requestHeaders = null;

        try {
            requestHeaders = session.getContext().getRequestHeaders();
        } catch (ContextNotActiveException e) {
            logger.debug("No active request, can't obtain locale from request");
        }

        AuthenticationSessionModel session = this.session.getContext().getAuthenticationSession();

        if (!realm.isInternationalizationEnabled()) {
            return Locale.ENGLISH;
        }

        Locale userLocale = getUserLocale(realm, session, user, requestHeaders, ignoreAcceptLanguageHeader);
        if (userLocale != null) {
            return userLocale;
        }

        String realmDefaultLocale = realm.getDefaultLocale();
        if (realmDefaultLocale != null) {
            return Locale.forLanguageTag(realmDefaultLocale);
        }

        return Locale.ENGLISH;
    }

    private Locale getUserLocale(RealmModel realm, AuthenticationSessionModel session, UserModel user, HttpHeaders requestHeaders, boolean ignoreAcceptLanguageHeader) {
        Locale locale;

        locale = getUserSelectedLocale(realm, session);
        if (locale != null) {
            return locale;
        }

        locale = getUserProfileSelection(realm, user);
        if (locale != null) {
            return locale;
        }

        locale = getClientSelectedLocale(realm, session);
        if (locale != null) {
            return locale;
        }

        locale = getLocaleCookieSelection(realm, requestHeaders);
        if (locale != null) {
            return locale;
        }

        locale = getAcceptLanguageHeaderLocale(realm, requestHeaders, ignoreAcceptLanguageHeader);
        if (locale != null) {
            return locale;
        }

        return null;
    }

    private Locale getUserSelectedLocale(RealmModel realm, AuthenticationSessionModel session) {
        String locale = session == null ? this.session.getAttribute(USER_REQUEST_LOCALE, String.class) : session.getAuthNote(USER_REQUEST_LOCALE);
        if (locale == null) {
            return null;
        }

        return findLocale(realm, locale);
    }

    private Locale getUserProfileSelection(RealmModel realm, UserModel user) {
        if (user == null) {
            return null;
        }

        String locale = user.getFirstAttribute(UserModel.LOCALE);
        if (locale == null) {
            return null;
        }

        return findLocale(realm, locale);
    }

    private Locale getClientSelectedLocale(RealmModel realm, AuthenticationSessionModel session) {
        if (session == null) {
            return null;
        }

        String locale = session.getClientNote(LocaleSelectorProvider.CLIENT_REQUEST_LOCALE);
        if (locale == null) {
            return null;
        }

        return findLocale(realm, locale.split(" "));
    }

    private Locale getLocaleCookieSelection(RealmModel realm, HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        String localeCookie = session.getProvider(CookieProvider.class).get(CookieType.LOCALE);
        if (localeCookie == null) {
            return null;
        }

        return findLocale(realm, localeCookie);
    }

    private Locale getAcceptLanguageHeaderLocale(RealmModel realm, HttpHeaders httpHeaders, boolean ignoreAcceptLanguageHeader) {

        if (ignoreAcceptLanguageHeader) {
            return null;
        }

        if (httpHeaders == null) {
            return null;
        }

        List<Locale> acceptableLanguages = httpHeaders.getAcceptableLanguages();
        if (acceptableLanguages == null || acceptableLanguages.isEmpty()) {
            return null;
        }

        for (Locale l : acceptableLanguages) {
            Locale locale = findLocale(realm, l.toLanguageTag());
            if (locale != null) {
                return locale;
            }
        }

        return null;
    }

    private Locale findLocale(RealmModel realm, String... localeStrings) {
        List<Locale> supportedLocales = realm.getSupportedLocalesStream()
                .map(Locale::forLanguageTag).collect(Collectors.toList());

        return findBestMatchingLocale(supportedLocales, localeStrings);
    }

    static Locale findBestMatchingLocale(List<Locale> supportedLocales, String... localeStrings) {
        for (String localeString : localeStrings) {
            if (localeString != null) {
                Locale result = null;
                Locale search = Locale.forLanguageTag(localeString);
                for (Locale supportedLocale : supportedLocales) {
                    if (doesLocaleMatch(search, supportedLocale) && (result == null
                            || doesFirstLocaleBetterMatchThanSecondLocale(supportedLocale, result, search))) {
                        result = supportedLocale;
                    }
                }
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static boolean doesLocaleMatch(Locale candidate, Locale supportedLocale) {
        return candidate.getLanguage().equals(supportedLocale.getLanguage())
                && ((candidate.getCountry().equals("") ^ supportedLocale.getCountry().equals(""))
                        || candidate.getCountry().equals(supportedLocale.getCountry()));
    }

    private static boolean doesFirstLocaleBetterMatchThanSecondLocale(Locale firstLocale, Locale secondLocale,
            Locale supportedLocale) {
        if (firstLocale.getLanguage().equals(supportedLocale.getLanguage())
                && !secondLocale.getLanguage().equals(supportedLocale.getLanguage())) {
            return true;
        }

        if (firstLocale.getCountry().equals(supportedLocale.getCountry())
                && !secondLocale.getCountry().equals(supportedLocale.getCountry())) {
            return true;
        }

        return firstLocale.getVariant().equals(supportedLocale.getVariant())
                && !secondLocale.getVariant().equals(supportedLocale.getVariant());
    }

    @Override
    public void close() {
    }

}
