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

import org.keycloak.OAuth2Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.util.CookieHelper;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.util.Locale;

public class DefaultLocaleSelectorProvider implements LocaleSelectorProvider {

    protected static final String LOCALE_COOKIE = "KEYCLOAK_LOCALE";
    protected static final String KC_LOCALE_PARAM = "kc_locale";

    protected final KeycloakSession session;

    public DefaultLocaleSelectorProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Locale resolveLocale(RealmModel realm, UserModel user) {
        final HttpHeaders requestHeaders = session.getContext().getRequestHeaders();
        final UriInfo uri = session.getContext().getUri();
        return getLocale(realm, user, requestHeaders, uri);
    }

    @Override
    public void close() {

    }

    protected Locale getLocale(RealmModel realm, UserModel user, HttpHeaders requestHeaders, UriInfo uriInfo) {
        if (!realm.isInternationalizationEnabled()) {
            return Locale.ENGLISH;
        } else {
            Locale locale = getUserLocale(realm, user, requestHeaders, uriInfo);
            return locale != null ? locale : Locale.forLanguageTag(realm.getDefaultLocale());
        }
    }

    protected Locale getUserLocale(RealmModel realm, UserModel user, HttpHeaders requestHeaders, UriInfo uriInfo) {
        final LocaleSelection kcLocaleQueryParamSelection = getKcLocaleQueryParamSelection(realm, uriInfo);
        if (kcLocaleQueryParamSelection != null) {
            updateLocaleCookie(realm, kcLocaleQueryParamSelection.getLocaleString(), uriInfo);
            if (user != null) {
                updateUsersLocale(user, kcLocaleQueryParamSelection.getLocaleString());
            }
            return kcLocaleQueryParamSelection.getLocale();
        }

        final LocaleSelection localeCookieSelection = getLocaleCookieSelection(realm, requestHeaders);
        if (localeCookieSelection != null) {
            if (user != null) {
                updateUsersLocale(user, localeCookieSelection.getLocaleString());
            }
            return localeCookieSelection.getLocale();
        }

        final LocaleSelection userProfileSelection = getUserProfileSelection(realm, user);
        if (userProfileSelection != null) {
            updateLocaleCookie(realm, userProfileSelection.getLocaleString(), uriInfo);
            return userProfileSelection.getLocale();
        }

        final LocaleSelection uiLocalesQueryParamSelection = getUiLocalesQueryParamSelection(realm, uriInfo);
        if (uiLocalesQueryParamSelection != null) {
            return uiLocalesQueryParamSelection.getLocale();
        }

        final LocaleSelection acceptLanguageHeaderSelection = getAcceptLanguageHeaderLocale(realm, requestHeaders);
        if (acceptLanguageHeaderSelection != null) {
            return acceptLanguageHeaderSelection.getLocale();
        }

        return null;
    }

    protected LocaleSelection getKcLocaleQueryParamSelection(RealmModel realm, UriInfo uriInfo) {
        if (uriInfo == null || !uriInfo.getQueryParameters().containsKey(KC_LOCALE_PARAM)) {
            return null;
        }
        String localeString = uriInfo.getQueryParameters().getFirst(KC_LOCALE_PARAM);
        return findLocale(realm, localeString);
    }

    protected LocaleSelection getLocaleCookieSelection(RealmModel realm, HttpHeaders httpHeaders) {
        if (httpHeaders == null || !httpHeaders.getCookies().containsKey(LOCALE_COOKIE)) {
            return null;
        }
        String localeString = httpHeaders.getCookies().get(LOCALE_COOKIE).getValue();
        return findLocale(realm, localeString);
    }

    protected LocaleSelection getUserProfileSelection(RealmModel realm, UserModel user) {
        if (user == null || !user.getAttributes().containsKey(UserModel.LOCALE)) {
            return null;
        }
        String localeString = user.getFirstAttribute(UserModel.LOCALE);
        return findLocale(realm, localeString);
    }

    protected LocaleSelection getUiLocalesQueryParamSelection(RealmModel realm, UriInfo uriInfo) {
        if (uriInfo == null || !uriInfo.getQueryParameters().containsKey(OAuth2Constants.UI_LOCALES_PARAM)) {
            return null;
        }
        String localeString = uriInfo.getQueryParameters().getFirst(OAuth2Constants.UI_LOCALES_PARAM);
        return findLocale(realm, localeString.split(" "));
    }

    protected LocaleSelection getAcceptLanguageHeaderLocale(RealmModel realm, HttpHeaders httpHeaders) {
        if (httpHeaders == null || httpHeaders.getAcceptableLanguages() == null || httpHeaders.getAcceptableLanguages().isEmpty()) {
            return null;
        }
        for (Locale l : httpHeaders.getAcceptableLanguages()) {
            String localeString = l.toLanguageTag();
            LocaleSelection localeSelection = findLocale(realm, localeString);
            if (localeSelection != null) {
                return localeSelection;
            }
        }
        return null;
    }

    protected void updateLocaleCookie(RealmModel realm, String locale, UriInfo uriInfo) {
        boolean secure = realm.getSslRequired().isRequired(uriInfo.getRequestUri().getHost());
        CookieHelper.addCookie(LOCALE_COOKIE, locale, AuthenticationManager.getRealmCookiePath(realm, uriInfo), null, null, -1, secure, true);
    }

    protected LocaleSelection findLocale(RealmModel realm, String... localeStrings) {
        return new LocaleNegotiator(realm.getSupportedLocales()).invoke(localeStrings);
    }

    protected void updateUsersLocale(UserModel user, String locale) {
        if (!locale.equals(user.getFirstAttribute("locale"))) {
            user.setSingleAttribute(UserModel.LOCALE, locale);
        }
    }

}
