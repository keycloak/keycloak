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

import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.locale.LocaleUpdaterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LocaleUtil {

    public static void processLocaleParam(KeycloakSession session, RealmModel realm, AuthenticationSessionModel authSession) {
        if (authSession != null && realm.isInternationalizationEnabled()) {
            String locale = session.getContext().getUri().getQueryParameters().getFirst(LocaleSelectorProvider.KC_LOCALE_PARAM);
            if (locale != null) {
                authSession.setAuthNote(LocaleSelectorProvider.USER_REQUEST_LOCALE, locale);

                LocaleUpdaterProvider localeUpdater = session.getProvider(LocaleUpdaterProvider.class);
                localeUpdater.updateLocaleCookie(locale);
            }
        }
    }
}
