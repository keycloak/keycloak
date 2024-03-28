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
import java.util.Optional;

/**
 * Represents a DisplayObject, as used in the OID4VCI Credentials Issuer Metadata
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-issuer-metadata}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
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
    @JsonIgnore
    private static final String LOGO_KEY = "logo";
    @JsonIgnore
    private static final String DESCRIPTION_KEY = "description";
    @JsonIgnore
    private static final String BG_COLOR_KEY = "background_color";
    @JsonIgnore
    private static final String TEXT_COLOR_KEY = "text_color";

    @JsonProperty(DisplayObject.NAME_KEY)
    private String name;

    @JsonProperty(DisplayObject.LOCALE_KEY)
    private String locale;

    @JsonProperty(DisplayObject.LOGO_KEY)
    private String logo;

    @JsonProperty(DisplayObject.DESCRIPTION_KEY)
    private String description;

    @JsonProperty(DisplayObject.BG_COLOR_KEY)
    private String backgroundColor;

    @JsonProperty(DisplayObject.TEXT_COLOR_KEY)
    private String textColor;


    public String getName() {
        return name;
    }

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

    public String getLogo() {
        return logo;
    }

    public DisplayObject setLogo(String logo) {
        this.logo = logo;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public DisplayObject setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public DisplayObject setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public String getTextColor() {
        return textColor;
    }

    public DisplayObject setTextColor(String textColor) {
        this.textColor = textColor;
        return this;
    }

    public Map<String, String> toDotNotation() {
        Map<String, String> dotNotation = new HashMap<>();
        dotNotation.put(NAME_KEY, name);
        dotNotation.put(LOCALE_KEY, locale);
        dotNotation.put(LOGO_KEY, logo);
        dotNotation.put(DESCRIPTION_KEY, description);
        dotNotation.put(BG_COLOR_KEY, backgroundColor);
        dotNotation.put(TEXT_COLOR_KEY, textColor);
        return dotNotation;
    }

    public static DisplayObject fromDotNotation(Map<String, String> dotNotated) {
        DisplayObject displayObject = new DisplayObject();
        Optional.ofNullable(dotNotated.get(NAME_KEY)).ifPresent(displayObject::setName);
        Optional.ofNullable(dotNotated.get(LOCALE_KEY)).ifPresent(displayObject::setLocale);
        Optional.ofNullable(dotNotated.get(LOGO_KEY)).ifPresent(displayObject::setLogo);
        Optional.ofNullable(dotNotated.get(DESCRIPTION_KEY)).ifPresent(displayObject::setDescription);
        Optional.ofNullable(dotNotated.get(BG_COLOR_KEY)).ifPresent(displayObject::setBackgroundColor);
        Optional.ofNullable(dotNotated.get(TEXT_COLOR_KEY)).ifPresent(displayObject::setTextColor);
        return displayObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DisplayObject that)) return false;

        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
        if (getLocale() != null ? !getLocale().equals(that.getLocale()) : that.getLocale() != null) return false;
        if (getLogo() != null ? !getLogo().equals(that.getLogo()) : that.getLogo() != null) return false;
        if (getDescription() != null ? !getDescription().equals(that.getDescription()) : that.getDescription() != null)
            return false;
        if (getBackgroundColor() != null ? !getBackgroundColor().equals(that.getBackgroundColor()) : that.getBackgroundColor() != null)
            return false;
        return getTextColor() != null ? getTextColor().equals(that.getTextColor()) : that.getTextColor() == null;
    }

    @Override
    public int hashCode() {
        int result = getName() != null ? getName().hashCode() : 0;
        result = 31 * result + (getLocale() != null ? getLocale().hashCode() : 0);
        result = 31 * result + (getLogo() != null ? getLogo().hashCode() : 0);
        result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
        result = 31 * result + (getBackgroundColor() != null ? getBackgroundColor().hashCode() : 0);
        result = 31 * result + (getTextColor() != null ? getTextColor().hashCode() : 0);
        return result;
    }
}