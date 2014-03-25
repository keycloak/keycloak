package org.keycloak.models.mongo.keycloak.entities;

import java.util.Map;

import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.MongoField;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationProviderEntity implements MongoEntity {

    private String providerName;
    private boolean passwordUpdateSupported;
    private Map<String, String> config;

    @MongoField
    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    @MongoField
    public boolean isPasswordUpdateSupported() {
        return passwordUpdateSupported;
    }

    public void setPasswordUpdateSupported(boolean passwordUpdateSupported) {
        this.passwordUpdateSupported = passwordUpdateSupported;
    }

    @MongoField
    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }
}
