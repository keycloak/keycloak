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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A supported credential, as used in the Credentials Issuer Metadata in OID4VCI
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-issuer-metadata}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupportedCredentialConfiguration {

    private static final String DOT_SEPARATOR = ".";

    @JsonIgnore
    private static final String FORMAT_KEY = "format";
    @JsonIgnore
    private static final String SCOPE_KEY = "scope";
    @JsonIgnore
    private static final String CRYPTOGRAPHIC_BINDING_METHODS_SUPPORTED_KEY = " credential_signing_alg_values_supported";
    @JsonIgnore
    private static final String CRYPTOGRAPHIC_SUITES_SUPPORTED_KEY = "cryptographic_suites_supported";
    @JsonIgnore
    private static final String CREDENTIAL_SIGNING_ALG_VALUES_SUPPORTED_KEY = "credential_signing_alg_values_supported";
    @JsonIgnore
    private static final String DISPLAY_KEY = "display";
    private String id;

    @JsonProperty(FORMAT_KEY)
    private Format format;

    @JsonProperty(SCOPE_KEY)
    private String scope;

    @JsonProperty(CRYPTOGRAPHIC_BINDING_METHODS_SUPPORTED_KEY)
    private List<String> cryptographicBindingMethodsSupported;

    @JsonProperty(CRYPTOGRAPHIC_SUITES_SUPPORTED_KEY)
    private List<String> cryptographicSuitesSupported;

    @JsonProperty(CREDENTIAL_SIGNING_ALG_VALUES_SUPPORTED_KEY)
    private List<String> credentialSigningAlgValuesSupported;

    @JsonProperty(DISPLAY_KEY)
    private DisplayObject display;

    public Format getFormat() {
        return format;
    }

    public SupportedCredentialConfiguration setFormat(Format format) {
        this.format = format;
        return this;
    }

    public String getScope() {
        return scope;
    }

    public SupportedCredentialConfiguration setScope(String scope) {
        this.scope = scope;
        return this;
    }

    public List<String> getCryptographicBindingMethodsSupported() {
        return cryptographicBindingMethodsSupported;
    }

    public SupportedCredentialConfiguration setCryptographicBindingMethodsSupported(List<String> cryptographicBindingMethodsSupported) {
        this.cryptographicBindingMethodsSupported = Collections.unmodifiableList(cryptographicBindingMethodsSupported);
        return this;
    }

    public List<String> getCryptographicSuitesSupported() {
        return cryptographicSuitesSupported;
    }

    public SupportedCredentialConfiguration setCryptographicSuitesSupported(List<String> cryptographicSuitesSupported) {
        this.cryptographicSuitesSupported = Collections.unmodifiableList(cryptographicSuitesSupported);
        return this;
    }

    public DisplayObject getDisplay() {
        return display;
    }

    public SupportedCredentialConfiguration setDisplay(DisplayObject display) {
        this.display = display;
        return this;
    }

    public String getId() {
        return id;
    }

    public SupportedCredentialConfiguration setId(String id) {
        if (id.contains(".")) {
            throw new IllegalArgumentException("dots are not supported as part of the supported credentials id.");
        }
        this.id = id;
        return this;
    }

    public List<String> getCredentialSigningAlgValuesSupported() {
        return credentialSigningAlgValuesSupported;
    }

    public SupportedCredentialConfiguration setCredentialSigningAlgValuesSupported(List<String> credentialSigningAlgValuesSupported) {
        this.credentialSigningAlgValuesSupported = Collections.unmodifiableList(credentialSigningAlgValuesSupported);
        return this;
    }

    public Map<String, String> toDotNotation() {
        Map<String, String> dotNotation = new HashMap<>();
        Optional.ofNullable(format).ifPresent(format -> dotNotation.put(id + DOT_SEPARATOR + FORMAT_KEY, format.toString()));
        Optional.ofNullable(scope).ifPresent(scope -> dotNotation.put(id + DOT_SEPARATOR + SCOPE_KEY, scope));
        Optional.ofNullable(cryptographicBindingMethodsSupported).ifPresent(types ->
                dotNotation.put(id + DOT_SEPARATOR + CRYPTOGRAPHIC_BINDING_METHODS_SUPPORTED_KEY, String.join(",", cryptographicBindingMethodsSupported)));
        Optional.ofNullable(cryptographicSuitesSupported).ifPresent(types ->
                dotNotation.put(id + DOT_SEPARATOR + CRYPTOGRAPHIC_SUITES_SUPPORTED_KEY, String.join(",", cryptographicSuitesSupported)));
        Optional.ofNullable(cryptographicSuitesSupported).ifPresent(types ->
                dotNotation.put(id + DOT_SEPARATOR + CREDENTIAL_SIGNING_ALG_VALUES_SUPPORTED_KEY, String.join(",", credentialSigningAlgValuesSupported)));

        Map<String, String> dotNotatedDisplay = Optional.ofNullable(display)
                .map(DisplayObject::toDotNotation)
                .orElse(Map.of());
        dotNotatedDisplay.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .forEach(entry -> dotNotation.put(id + DOT_SEPARATOR + DISPLAY_KEY + "." + entry.getKey(), entry.getValue()));
        return dotNotation;
    }

    public static SupportedCredentialConfiguration fromDotNotation(String credentialId, Map<String, String> dotNotated) {

        SupportedCredentialConfiguration supportedCredentialConfiguration = new SupportedCredentialConfiguration().setId(credentialId);
        Optional.ofNullable(dotNotated.get(credentialId + DOT_SEPARATOR + FORMAT_KEY)).map(Format::fromString).ifPresent(supportedCredentialConfiguration::setFormat);
        Optional.ofNullable(dotNotated.get(credentialId + DOT_SEPARATOR + SCOPE_KEY)).ifPresent(supportedCredentialConfiguration::setScope);
        Optional.ofNullable(dotNotated.get(credentialId + DOT_SEPARATOR + CRYPTOGRAPHIC_BINDING_METHODS_SUPPORTED_KEY))
                .map(cbms -> cbms.split(","))
                .map(Arrays::asList)
                .ifPresent(supportedCredentialConfiguration::setCryptographicBindingMethodsSupported);
        Optional.ofNullable(dotNotated.get(credentialId + DOT_SEPARATOR + CRYPTOGRAPHIC_SUITES_SUPPORTED_KEY))
                .map(css -> css.split(","))
                .map(Arrays::asList)
                .ifPresent(supportedCredentialConfiguration::setCryptographicSuitesSupported);
        Optional.ofNullable(dotNotated.get(credentialId + DOT_SEPARATOR + CREDENTIAL_SIGNING_ALG_VALUES_SUPPORTED_KEY))
                .map(css -> css.split(","))
                .map(Arrays::asList)
                .ifPresent(supportedCredentialConfiguration::setCredentialSigningAlgValuesSupported);
        Map<String, String> displayMap = new HashMap<>();
        dotNotated.entrySet().forEach(entry -> {
            String key = entry.getKey();
            if (key.startsWith(credentialId + DOT_SEPARATOR + DISPLAY_KEY)) {
                displayMap.put(key.substring((credentialId + DOT_SEPARATOR + DISPLAY_KEY).length() + 1), entry.getValue());
            }
        });
        if (!displayMap.isEmpty()) {
            supportedCredentialConfiguration.setDisplay(DisplayObject.fromDotNotation(displayMap));
        }
        return supportedCredentialConfiguration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SupportedCredentialConfiguration that)) return false;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getFormat() != that.getFormat()) return false;
        if (getScope() != null ? !getScope().equals(that.getScope()) : that.getScope() != null) return false;
        if (getCryptographicBindingMethodsSupported() != null ? !getCryptographicBindingMethodsSupported().equals(that.getCryptographicBindingMethodsSupported()) : that.getCryptographicBindingMethodsSupported() != null)
            return false;
        if (getCryptographicSuitesSupported() != null ? !getCryptographicSuitesSupported().equals(that.getCryptographicSuitesSupported()) : that.getCryptographicSuitesSupported() != null)
            return false;
        if (getCredentialSigningAlgValuesSupported() != null ? !getCredentialSigningAlgValuesSupported().equals(that.getCredentialSigningAlgValuesSupported()) : that.getCredentialSigningAlgValuesSupported() != null)
            return false;
        return getDisplay() != null ? getDisplay().equals(that.getDisplay()) : that.getDisplay() == null;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getFormat() != null ? getFormat().hashCode() : 0);
        result = 31 * result + (getScope() != null ? getScope().hashCode() : 0);
        result = 31 * result + (getCryptographicBindingMethodsSupported() != null ? getCryptographicBindingMethodsSupported().hashCode() : 0);
        result = 31 * result + (getCryptographicSuitesSupported() != null ? getCryptographicSuitesSupported().hashCode() : 0);
        result = 31 * result + (getCredentialSigningAlgValuesSupported() != null ? getCredentialSigningAlgValuesSupported().hashCode() : 0);
        result = 31 * result + (getDisplay() != null ? getDisplay().hashCode() : 0);
        return result;
    }
}