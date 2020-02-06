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
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.storage.ReadOnlyException;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DefaultLocaleSelectorProvider implements LocaleSelectorProvider {

    private static final Logger logger = Logger.getLogger(LocaleSelectorProvider.class);

    private KeycloakSession session;

    public DefaultLocaleSelectorProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Locale resolveLocale(UserModel user) {
        RealmModel realm = session.getContext().getRealm();
        HttpHeaders requestHeaders = session.getContext().getRequestHeaders();
        AuthenticationSessionModel session = this.session.getContext().getAuthenticationSession();

        if (!realm.isInternationalizationEnabled()) {
            return Locale.ENGLISH;
        }

        Locale userLocale = getUserLocale(realm, session, user, requestHeaders);
        if (userLocale != null) {
            return userLocale;
        }

        String realmDefaultLocale = realm.getDefaultLocale();
        if (realmDefaultLocale != null) {
            return Locale.forLanguageTag(realmDefaultLocale);
        }

        return Locale.ENGLISH;
    }

    public void updateUsersLocale(UserModel user, String locale) {
        if (!locale.equals(user.getFirstAttribute("locale"))) {
            try {
                user.setSingleAttribute(UserModel.LOCALE, locale);
                updateLocaleCookie(session.getContext().getRealm(), locale, session.getContext().getUri());
            } catch (ReadOnlyException e) {
                logger.debug("Attempt to store 'locale' attribute to read only user model. Ignoring exception", e);
            }
        }
        logger.debugv("Setting locale for user {0} to {1}", user.getUsername(), locale);
    }

    public void updateLocaleCookie(RealmModel realm, String locale, UriInfo uriInfo) {
        boolean secure = realm.getSslRequired().isRequired(uriInfo.getRequestUri().getHost());
        CookieHelper.addCookie(LocaleSelectorProvider.LOCALE_COOKIE, locale, AuthenticationManager.getRealmCookiePath(realm, uriInfo), null, null, -1, secure, true);
        logger.debugv("Updating locale cookie to {0}", locale);
    }

    public void expireLocaleCookie(RealmModel realm, UriInfo uriInfo) {
        boolean secure = realm.getSslRequired().isRequired(uriInfo.getRequestUri().getHost());
        CookieHelper.addCookie(LocaleSelectorProvider.LOCALE_COOKIE, "", AuthenticationManager.getRealmCookiePath(realm, uriInfo), null, "Expiring cookie", 0, secure, true);
    }

    private Locale getUserLocale(RealmModel realm, AuthenticationSessionModel session, UserModel user, HttpHeaders requestHeaders) {
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

        locale = getAcceptLanguageHeaderLocale(realm, requestHeaders);
        if (locale != null) {
            return locale;
        }

        return null;
    }

    private Locale getUserSelectedLocale(RealmModel realm, AuthenticationSessionModel session) {
        if (session == null) {
            return null;
        }

        String locale = session.getAuthNote(USER_REQUEST_LOCALE);
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

        String locale = session.getAuthNote(LocaleSelectorProvider.CLIENT_REQUEST_LOCALE);
        if (locale == null) {
            return null;
        }

        return findLocale(realm, locale.split(" "));
    }

    private Locale getLocaleCookieSelection(RealmModel realm, HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Cookie localeCookie = httpHeaders.getCookies().get(LOCALE_COOKIE);
        if (localeCookie == null) {
            return null;
        }

        return findLocale(realm, localeCookie.getValue());
    }

    private Locale getAcceptLanguageHeaderLocale(RealmModel realm, HttpHeaders httpHeaders) {
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
        Set<String> supportedLocales = realm.getSupportedLocales();
        for (String localeString : localeStrings) {
            if (localeString != null) {
                Locale result = null;
                Locale search = Locale.forLanguageTag(localeString);
                for (String languageTag : supportedLocales) {
                    Locale locale = Locale.forLanguageTag(languageTag);
                    if (locale.getLanguage().equals(search.getLanguage())) {
                        if (search.getCountry().equals("") ^ locale.getCountry().equals("") && result == null) {
                            result = locale;
                        }
                        if (locale.getCountry().equals(search.getCountry())) {
                            return locale;
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
