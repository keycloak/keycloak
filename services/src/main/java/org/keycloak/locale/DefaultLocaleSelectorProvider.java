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

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DefaultLocaleSelectorProvider implements LocaleSelectorProvider {

    private static final Logger logger = Logger.getLogger(LocaleSelectorProvider.class);

    protected KeycloakSession session;

    public DefaultLocaleSelectorProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Locale resolveLocale(RealmModel realm, UserModel user) {
        HttpHeaders requestHeaders = session.getContext().getRequestHeaders();
        KeycloakUriInfo kcURIInfo = session.getContext().getUri();
        AuthenticationSessionModel session = this.session.getContext().getAuthenticationSession();

        if (!realm.isInternationalizationEnabled()) {
            return Locale.ENGLISH;
        }

        Locale userLocale = getUserLocale(realm, session, user, kcURIInfo, requestHeaders);
        if (userLocale != null) {
            return userLocale;
        }

        String realmDefaultLocale = realm.getDefaultLocale();
        if (realmDefaultLocale != null) {
            return Locale.forLanguageTag(realmDefaultLocale);
        }

        return Locale.ENGLISH;
    }

    protected Locale getUserLocale(RealmModel realm, AuthenticationSessionModel session, UserModel user, KeycloakUriInfo kcURIInfo, HttpHeaders requestHeaders) {
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

        locale = getLocaleKCLocaleParameter(realm, kcURIInfo);
        if (locale != null) {
            return locale;
        }

        locale = getAcceptLanguageHeaderLocale(realm, requestHeaders);
        if (locale != null) {
            return locale;
        }

        return null;
    }

    protected Locale getUserSelectedLocale(RealmModel realm, AuthenticationSessionModel session) {
        if (session == null) {
            return null;
        }

        String locale = session.getAuthNote(USER_REQUEST_LOCALE);
        if (locale == null) {
            return null;
        }

        return findLocale(realm, locale);
    }

    protected Locale getUserProfileSelection(RealmModel realm, UserModel user) {
        if (user == null) {
            return null;
        }

        String locale = user.getFirstAttribute(UserModel.LOCALE);
        if (locale == null) {
            return null;
        }

        return findLocale(realm, locale);
    }

    protected Locale getClientSelectedLocale(RealmModel realm, AuthenticationSessionModel session) {
        if (session == null) {
            return null;
        }

        String locale = session.getAuthNote(LocaleSelectorProvider.CLIENT_REQUEST_LOCALE);
        if (locale == null) {
            return null;
        }

        return findLocale(realm, locale.split(" "));
    }

    protected Locale getLocaleCookieSelection(RealmModel realm, HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Cookie localeCookie = httpHeaders.getCookies().get(LOCALE_COOKIE);
        if (localeCookie == null) {
            return null;
        }

        return findLocale(realm, localeCookie.getValue());
    }

    protected Locale getLocaleKCLocaleParameter(RealmModel realm, KeycloakUriInfo kcURIInfo) {
        if (kcURIInfo == null) {
            return null;
        }

        String kcLocale = kcURIInfo.getQueryParameters().getFirst(KC_LOCALE_PARAM);
        if (kcLocale == null) {
            return null;
        }

        return findLocale(realm, kcLocale);
    }

    protected Locale getAcceptLanguageHeaderLocale(RealmModel realm, HttpHeaders httpHeaders) {
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

    protected Locale findLocale(RealmModel realm, String... localeStrings) {
        List<Locale> supportedLocales = realm.getSupportedLocalesStream()
                .map(Locale::forLanguageTag).collect(Collectors.toList());
        for (String localeString : localeStrings) {
            if (localeString != null) {
                Locale result = null;
                Locale search = Locale.forLanguageTag(localeString);
                for (Locale supportedLocale : supportedLocales) {
                    if (supportedLocale.getLanguage().equals(search.getLanguage())) {
                        if (search.getCountry().equals("") ^ supportedLocale.getCountry().equals("") && result == null) {
                            result = supportedLocale;
                        }
                        if (supportedLocale.getCountry().equals(search.getCountry())) {
                            return supportedLocale;
                        }
                    }
                }
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    @Override
    public void close() {
    }

}
