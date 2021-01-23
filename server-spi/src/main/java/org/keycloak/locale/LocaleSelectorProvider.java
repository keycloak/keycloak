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

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

import java.util.Locale;

public interface LocaleSelectorProvider extends Provider {

    String LOCALE_COOKIE = "KEYCLOAK_LOCALE";
    String KC_LOCALE_PARAM = "kc_locale";

    String CLIENT_REQUEST_LOCALE = "locale_client_requested";
    String USER_REQUEST_LOCALE = "locale_user_requested";

    /**
     * Resolve the locale which should be used for the request
     * @param user
     * @return
     */
    Locale resolveLocale(RealmModel realm, UserModel user);

}