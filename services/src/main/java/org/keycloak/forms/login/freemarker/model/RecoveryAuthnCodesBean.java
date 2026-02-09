package org.keycloak.forms.login.freemarker.model;

import java.util.List;

import org.keycloak.common.util.Time;
import org.keycloak.models.utils.RecoveryAuthnCodesUtils;

public class RecoveryAuthnCodesBean {

    private final List<String> generatedRecoveryAuthnCodesList;
    private final long generatedAt;

    public RecoveryAuthnCodesBean() {
        this.generatedRecoveryAuthnCodesList = RecoveryAuthnCodesUtils.generateRawCodes();
        this.generatedAt = Time.currentTimeMillis();
    }

    public List<String> getGeneratedRecoveryAuthnCodesList() {
        return this.generatedRecoveryAuthnCodesList;
    }

    public String getGeneratedRecoveryAuthnCodesAsString() {
        return String.join(",", this.generatedRecoveryAuthnCodesList);
    }

    public long getGeneratedAt() {
        return generatedAt;
    }

}
