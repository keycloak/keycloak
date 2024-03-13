/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
public class DisplayObject {

    @JsonIgnore
    private static final String NAME_KEY = "name";
    @JsonIgnore
    private static final String LOCALE_KEY = "locale";

    @JsonProperty(DisplayObject.NAME_KEY)
    private String name;

    @JsonProperty(DisplayObject.LOCALE_KEY)
    public String getName() {
        return name;
    }

    private String locale;

    public DisplayObject setName(String name) {
        this.name = name;
        return this;
    }

    public String getLocale() {
        return locale;
    }

    public DisplayObject setLocale(String locale) {
        this.locale = locale;
        return this;
    }

    public Map<String, String> toDotNotation() {
        Map<String, String> dotNotation = new HashMap<>();
        dotNotation.put(NAME_KEY, name);
        dotNotation.put(LOCALE_KEY, locale);
        return dotNotation;
    }

    public static DisplayObject fromDotNotation(Map<String, String> dotNotated) {
        DisplayObject displayObject = new DisplayObject();
        Optional.ofNullable(dotNotated.get(NAME_KEY)).ifPresent(displayObject::setName);
        Optional.ofNullable(dotNotated.get(LOCALE_KEY)).ifPresent(displayObject::setLocale);
        return displayObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DisplayObject that)) return false;

        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
        return getLocale() != null ? getLocale().equals(that.getLocale()) : that.getLocale() == null;
    }

    @Override
    public int hashCode() {
        int result = getName() != null ? getName().hashCode() : 0;
        result = 31 * result + (getLocale() != null ? getLocale().hashCode() : 0);
        return result;
    }
}