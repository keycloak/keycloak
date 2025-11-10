package org.keycloak.verifiableclaims;

import java.util.Collections;
import java.util.Map;

public class VerifiableClaimException extends RuntimeException {
    private final String i18nKey;
    private final Map<String, Object> params;

    public VerifiableClaimException(String i18nKey, String message) {
        super(message);
        this.i18nKey = i18nKey;
        this.params = Collections.emptyMap();
    }

    public VerifiableClaimException(String i18nKey, String message, Map<String, Object> params) {
        super(message);
        this.i18nKey = i18nKey;
        this.params = params == null ? Collections.emptyMap() : params;
    }

    public String getI18nKey() { return i18nKey; }
    public Map<String, Object> getParams() { return params; }
}
