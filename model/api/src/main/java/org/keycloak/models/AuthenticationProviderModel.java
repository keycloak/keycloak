package org.keycloak.models;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationProviderModel {

    public static final AuthenticationProviderModel DEFAULT_PROVIDER = new AuthenticationProviderModel("model", true, Collections.EMPTY_MAP);

    private String providerName;
    private boolean passwordUpdateSupported = true;
    private Map<String, String> config = new HashMap<String, String>();

    public AuthenticationProviderModel() {};

    public AuthenticationProviderModel(String providerName, boolean passwordUpdateSupported, Map<String, String> config) {
        this.providerName = providerName;
        this.passwordUpdateSupported = passwordUpdateSupported;
        if (config != null) {
           this.config.putAll(config);
        }
    }

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
