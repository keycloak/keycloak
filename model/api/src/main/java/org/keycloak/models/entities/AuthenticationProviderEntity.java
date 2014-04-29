package org.keycloak.models.entities;

import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationProviderEntity {

    private String providerName;
    private boolean passwordUpdateSupported;
    private Map<String, String> config;

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public boolean isPasswordUpdateSupported() {
        return passwordUpdateSupported;
    }

    public void setPasswordUpdateSupported(boolean passwordUpdateSupported) {
        this.passwordUpdateSupported = passwordUpdateSupported;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }
}
