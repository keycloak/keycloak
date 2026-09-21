/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.util.Collections;
import java.util.List;

public class ThemeResources {

    private static final ThemeResources EMPTY = new ThemeResources(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
    );

    private final List<ThemeResourceDescriptor> styles;
    private final List<ThemeResourceDescriptor> stylesCommon;
    private final List<ThemeResourceDescriptor> scripts;
    private final List<ThemeResourceDescriptor> favicons;

    public ThemeResources(
            List<ThemeResourceDescriptor> styles,
            List<ThemeResourceDescriptor> stylesCommon,
            List<ThemeResourceDescriptor> scripts,
            List<ThemeResourceDescriptor> favicons) {
        this.styles = List.copyOf(styles);
        this.stylesCommon = List.copyOf(stylesCommon);
        this.scripts = List.copyOf(scripts);
        this.favicons = List.copyOf(favicons);
    }

    public static ThemeResources empty() {
        return EMPTY;
    }

    public List<ThemeResourceDescriptor> getStyles() {
        return styles;
    }

    public List<ThemeResourceDescriptor> getStylesCommon() {
        return stylesCommon;
    }

    public List<ThemeResourceDescriptor> getScripts() {
        return scripts;
    }

    public List<ThemeResourceDescriptor> getFavicons() {
        return favicons;
    }
}
