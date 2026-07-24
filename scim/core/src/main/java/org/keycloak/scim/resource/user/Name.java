package org.keycloak.scim.resource.user;

import java.util.StringJoiner;

import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Name {

    @JsonProperty("formatted")
    private String formatted;

    @JsonProperty("familyName")
    private String familyName;

    @JsonProperty("givenName")
    private String givenName;

    @JsonProperty("middleName")
    private String middleName;

    @JsonProperty("honorificPrefix")
    private String honorificPrefix;

    @JsonProperty("honorificSuffix")
    private String honorificSuffix;

    public String getFormatted() {
        if (formatted == null) {
            StringJoiner joiner = new StringJoiner(" ");
            addIfNotBlank(joiner, honorificPrefix);
            addIfNotBlank(joiner, givenName);
            addIfNotBlank(joiner, middleName);
            addIfNotBlank(joiner, familyName);
            addIfNotBlank(joiner, honorificSuffix);
            formatted = joiner.toString();
        }

        return StringUtil.isBlank(formatted.trim()) ? null : formatted;
    }

    private static void addIfNotBlank(StringJoiner joiner, String value) {
        if (!StringUtil.isBlank(value)) {
            joiner.add(value);
        }
    }

    public void setFormatted(String formatted) {
        this.formatted = formatted;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getHonorificPrefix() {
        return honorificPrefix;
    }

    public void setHonorificPrefix(String honorificPrefix) {
        this.honorificPrefix = honorificPrefix;
    }

    public String getHonorificSuffix() {
        return honorificSuffix;
    }

    public void setHonorificSuffix(String honorificSuffix) {
        this.honorificSuffix = honorificSuffix;
    }
}
