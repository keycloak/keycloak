package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupportedCredential {

    private static final String DOT_SEPERATOR = ".";

    @JsonIgnore
    private static final String FORMAT_KEY = "format";
    @JsonIgnore
    private static final String TYPES_KEY = "types";
    @JsonIgnore
    private static final String CRYPTOGRAPHIC_BINDING_METHODS_SUPPORTED_KEY = "cryptographic_binding_methods_supported";
    @JsonIgnore
    private static final String CRYPTOGRAPHIC_SUITES_SUPPORTED_KEY = "cryptographic_suites_supported";
    @JsonIgnore
    private static final String DISPLAY_KEY = "display";
    @JsonIgnore
    private static final String EXPIRY_KEY = "expiry_in_s";

    private String id;

    @JsonProperty(FORMAT_KEY)
    private Format format;

    @JsonProperty(TYPES_KEY)
    private List<String> types;

    @JsonProperty(CRYPTOGRAPHIC_BINDING_METHODS_SUPPORTED_KEY)
    private List<String> cryptographicBindingMethodsSupported;

    @JsonProperty(CRYPTOGRAPHIC_SUITES_SUPPORTED_KEY)
    private List<String> cryptographicSuitesSupported;

    @JsonProperty(DISPLAY_KEY)
    private DisplayObject display;

    @JsonProperty(EXPIRY_KEY)
    private Long expiryInSeconds;

    public Format getFormat() {
        return format;
    }

    public SupportedCredential setFormat(Format format) {
        this.format = format;
        return this;
    }

    public List<String> getTypes() {
        return types;
    }

    public SupportedCredential setTypes(List<String> types) {
        this.types = types;
        return this;
    }

    public List<String> getCryptographicBindingMethodsSupported() {
        return cryptographicBindingMethodsSupported;
    }

    public SupportedCredential setCryptographicBindingMethodsSupported(List<String> cryptographicBindingMethodsSupported) {
        this.cryptographicBindingMethodsSupported = cryptographicBindingMethodsSupported;
        return this;
    }

    public List<String> getCryptographicSuitesSupported() {
        return cryptographicSuitesSupported;
    }

    public SupportedCredential setCryptographicSuitesSupported(List<String> cryptographicSuitesSupported) {
        this.cryptographicSuitesSupported = cryptographicSuitesSupported;
        return this;
    }

    public DisplayObject getDisplay() {
        return display;
    }

    public SupportedCredential setDisplay(DisplayObject display) {
        this.display = display;
        return this;
    }

    public String getId() {
        return id;
    }

    public SupportedCredential setId(String id) {
        if (id.contains(".")) {
            throw new IllegalArgumentException("dots are not supported as part of the supported credentials id.");
        }
        this.id = id;
        return this;
    }

    public Long getExpiryInSeconds() {
        return expiryInSeconds;
    }

    public SupportedCredential setExpiryInSeconds(Long expiryInSeconds) {
        this.expiryInSeconds = expiryInSeconds;
        return this;
    }

    public Map<String, String> toDotNotation() {
        Map<String, String> dotNotation = new HashMap<>();
        Optional.ofNullable(format).ifPresent(format -> dotNotation.put(id + DOT_SEPERATOR + FORMAT_KEY, format.toString()));
        Optional.ofNullable(types).ifPresent(types -> dotNotation.put(id + DOT_SEPERATOR + TYPES_KEY, String.join(",", types)));
        Optional.ofNullable(cryptographicBindingMethodsSupported).ifPresent(types ->
                dotNotation.put(id + DOT_SEPERATOR + CRYPTOGRAPHIC_BINDING_METHODS_SUPPORTED_KEY, String.join(",", cryptographicBindingMethodsSupported)));
        Optional.ofNullable(cryptographicSuitesSupported).ifPresent(types ->
                dotNotation.put(id + DOT_SEPERATOR + CRYPTOGRAPHIC_SUITES_SUPPORTED_KEY, String.join(",", cryptographicSuitesSupported)));
        Optional.ofNullable(expiryInSeconds).ifPresent(expiryInSeconds -> dotNotation.put(id + DOT_SEPERATOR + EXPIRY_KEY, String.valueOf(expiryInSeconds)));

        Map<String, String> dotNotatedDisplay = Optional.ofNullable(display)
                .map(DisplayObject::toDotNotation)
                .orElse(Map.of());
        dotNotatedDisplay.forEach((key, value) -> dotNotation.put(id + DOT_SEPERATOR + DISPLAY_KEY + "." + key, value));
        return dotNotation;
    }

    public static SupportedCredential fromDotNotation(String credentialId, Map<String, String> dotNotated) {

        SupportedCredential supportedCredential = new SupportedCredential().setId(credentialId);
        Optional.ofNullable(dotNotated.get(credentialId + DOT_SEPERATOR + FORMAT_KEY)).map(Format::fromString).ifPresent(supportedCredential::setFormat);
        Optional.ofNullable(dotNotated.get(credentialId + DOT_SEPERATOR + EXPIRY_KEY)).map(Long::valueOf).ifPresent(supportedCredential::setExpiryInSeconds);
        Optional.ofNullable(dotNotated.get(credentialId + DOT_SEPERATOR + TYPES_KEY))
                .map(types -> types.split(","))
                .map(Arrays::asList)
                .ifPresent(supportedCredential::setTypes);
        Optional.ofNullable(dotNotated.get(credentialId + DOT_SEPERATOR + CRYPTOGRAPHIC_BINDING_METHODS_SUPPORTED_KEY))
                .map(cbms -> cbms.split(","))
                .map(Arrays::asList)
                .ifPresent(supportedCredential::setCryptographicBindingMethodsSupported);
        Optional.ofNullable(dotNotated.get(credentialId + DOT_SEPERATOR + CRYPTOGRAPHIC_SUITES_SUPPORTED_KEY))
                .map(css -> css.split(","))
                .map(Arrays::asList)
                .ifPresent(supportedCredential::setCryptographicSuitesSupported);
        Map<String, String> displayMap = new HashMap<>();
        dotNotated.entrySet().forEach(entry -> {
            String key = entry.getKey();
            if (key.startsWith(credentialId + DOT_SEPERATOR + DISPLAY_KEY)) {
                displayMap.put(key.substring((credentialId + DOT_SEPERATOR + DISPLAY_KEY).length() + 1), entry.getValue());
            }
        });
        if (!displayMap.isEmpty()) {
            supportedCredential.setDisplay(DisplayObject.fromDotNotation(displayMap));
        }
        return supportedCredential;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SupportedCredential that)) return false;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getFormat() != that.getFormat()) return false;
        if (getTypes() != null ? !getTypes().equals(that.getTypes()) : that.getTypes() != null) return false;
        if (getCryptographicBindingMethodsSupported() != null ? !getCryptographicBindingMethodsSupported().equals(that.getCryptographicBindingMethodsSupported()) : that.getCryptographicBindingMethodsSupported() != null)
            return false;
        if (getCryptographicSuitesSupported() != null ? !getCryptographicSuitesSupported().equals(that.getCryptographicSuitesSupported()) : that.getCryptographicSuitesSupported() != null)
            return false;
        if (getDisplay() != null ? !getDisplay().equals(that.getDisplay()) : that.getDisplay() != null) return false;
        return getExpiryInSeconds() != null ? getExpiryInSeconds().equals(that.getExpiryInSeconds()) : that.getExpiryInSeconds() == null;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getFormat() != null ? getFormat().hashCode() : 0);
        result = 31 * result + (getTypes() != null ? getTypes().hashCode() : 0);
        result = 31 * result + (getCryptographicBindingMethodsSupported() != null ? getCryptographicBindingMethodsSupported().hashCode() : 0);
        result = 31 * result + (getCryptographicSuitesSupported() != null ? getCryptographicSuitesSupported().hashCode() : 0);
        result = 31 * result + (getDisplay() != null ? getDisplay().hashCode() : 0);
        result = 31 * result + (getExpiryInSeconds() != null ? getExpiryInSeconds().hashCode() : 0);
        return result;
    }
}