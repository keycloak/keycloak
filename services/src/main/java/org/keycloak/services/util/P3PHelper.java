/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.util;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.validation.Validation;
import org.keycloak.theme.Theme;
import org.keycloak.theme.ThemeProvider;

import java.io.IOException;
import java.util.Locale;

/**
 * IE requires P3P header to allow loading cookies from iframes when domain differs from main page (see KEYCLOAK-2828 for more details)
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class P3PHelper {

    private static final Logger logger = Logger.getLogger(P3PHelper.class);

    public static void addP3PHeader(KeycloakSession session) {
        try {
            ThemeProvider themeProvider = session.getProvider(ThemeProvider.class, "extending");
            Theme theme = themeProvider.getTheme(session.getContext().getRealm().getLoginTheme(), Theme.Type.LOGIN);

            Locale locale = LocaleHelper.getLocaleFromCookie(session);
            String p3pValue = theme.getMessages(locale).getProperty("p3pPolicy");

            if (!Validation.isBlank(p3pValue)) {
                HttpResponse response = ResteasyProviderFactory.getContextData(HttpResponse.class);
                response.getOutputHeaders().putSingle("P3P", p3pValue);
            }
        } catch (IOException e) {
            logger.error("Failed to set P3P header", e);
            return;
        }
    }

}
