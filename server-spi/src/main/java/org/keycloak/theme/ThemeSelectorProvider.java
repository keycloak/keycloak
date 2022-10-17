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

package org.keycloak.theme;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.Version;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface ThemeSelectorProvider extends Provider {

    /**
     * Return the theme name to use for the specified type
     *
     * @param type
     * @return
     */
    String getThemeName(Theme.Type type);

    default String getDefaultThemeName(Theme.Type type) {
        String name = Config.scope("theme").get("default", Version.NAME.toLowerCase());
        if ((type == Theme.Type.ACCOUNT) && Profile.isFeatureEnabled(Profile.Feature.ACCOUNT2)) {
            name = name.concat(".v2");
        } else if ((type == Theme.Type.ADMIN) && Profile.isFeatureEnabled(Profile.Feature.ADMIN2)) {
            name = name.concat(".v2");
        }
        return name;
    }

}
