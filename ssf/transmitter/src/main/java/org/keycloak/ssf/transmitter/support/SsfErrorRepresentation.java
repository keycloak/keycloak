package org.keycloak.ssf.transmitter.support;

import java.util.Map;

import org.keycloak.representations.idm.OAuth2ErrorRepresentation;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * OAuth2-style error body extended with an optional {@code params}
 * map of structured fields tied to the specific error code. Callers
 * (e.g. the admin UI) use it to parameterize a translated message
 * without having to parse {@code error_description}. Omitted from
 * the JSON when null/empty so simple errors keep the standard
 * {@code {error, error_description}} shape.
 */
public class SsfErrorRepresentation extends OAuth2ErrorRepresentation {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, String> params;

    public SsfErrorRepresentation() {}

    public SsfErrorRepresentation(String error, String errorDescription) {
        super(error, errorDescription);
    }

    public SsfErrorRepresentation(String error, String errorDescription, Map<String, String> params) {
        super(error, errorDescription);
        this.params = params;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }
}
