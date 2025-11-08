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

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(DisplayObject.class);

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
    @JsonIgnore
    private static final String BG_IMAGE_KEY = "background_image";

    @JsonProperty(DisplayObject.NAME_KEY)
    private String name;

    @JsonProperty(DisplayObject.LOCALE_KEY)
    private String locale;

    @JsonProperty(DisplayObject.LOGO_KEY)
    private LogoObject logo;

    @JsonProperty(DisplayObject.DESCRIPTION_KEY)
    private String description;

    @JsonProperty(DisplayObject.BG_COLOR_KEY)
    private String backgroundColor;

    @JsonProperty(DisplayObject.TEXT_COLOR_KEY)
    private String textColor;

    @JsonProperty(DisplayObject.BG_IMAGE_KEY)
    private BackgroundImageObject backgroundImage;

    public static List<DisplayObject> parse(CredentialScopeModel credentialScope) {
        String display = credentialScope.getVcDisplay();
        if (StringUtil.isBlank(display)) {
            return null;
        }
        TypeReference<List<DisplayObject>> typeReference = new TypeReference<>() {};
        try {
            return JsonSerialization.mapper.readValue(display, typeReference);
        } catch (JsonProcessingException e) {
            // lets say we have an invalid value we should not kill the whole execution if just the display value is
            // broken
            LOGGER.debug(e.getMessage(), e);
            LOGGER.info(String.format("Failed to parse display-metadata for credential: %s", credentialScope.getName()),
                        e.getMessage());
            return null;
        }
    }

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

    public LogoObject getLogo() {
        return logo;
    }

    public DisplayObject setLogo(LogoObject logo) {
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

    public BackgroundImageObject getBackgroundImage() {
        return backgroundImage;
    }

    public DisplayObject setBackgroundImage(BackgroundImageObject backgroundImage) {
        this.backgroundImage = backgroundImage;
        return this;
    }

    public String toJsonString(){
        try {
            return JsonSerialization.writeValueAsString(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static DisplayObject fromJsonString(String jsonString){
        try {
            return JsonSerialization.readValue(jsonString, DisplayObject.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        if (getBackgroundImage() != null ? !getBackgroundImage().equals(that.getBackgroundImage()) : that.getBackgroundImage() != null)
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
        result = 31 * result + (getBackgroundImage() != null ? getBackgroundImage().hashCode() : 0);
        result = 31 * result + (getTextColor() != null ? getTextColor().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        try {
            return JsonSerialization.mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Represents a logo object as defined in the OID4VCI specification.
     * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-issuer-metadata-p}
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LogoObject {
        @JsonProperty("uri")
        private String uri;

        @JsonProperty("alt_text")
        private String altText;

        public String getUri() {
            return uri;
        }

        public LogoObject setUri(String uri) {
            this.uri = uri;
            return this;
        }

        public String getAltText() {
            return altText;
        }

        public LogoObject setAltText(String altText) {
            this.altText = altText;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LogoObject that)) return false;
            return Objects.equals(uri, that.uri) && Objects.equals(altText, that.altText);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri, altText);
        }
    }

    /**
     * Represents a background image object as defined in the OID4VCI specification.
     * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-issuer-metadata-p}
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BackgroundImageObject {
        @JsonProperty("uri")
        private String uri;

        public String getUri() {
            return uri;
        }

        public BackgroundImageObject setUri(String uri) {
            this.uri = uri;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BackgroundImageObject that)) return false;
            return Objects.equals(uri, that.uri);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri);
        }
    }
}
