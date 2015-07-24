package org.keycloak.services.resources.admin.info;

import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SpiInfoRepresentation {

    private boolean internal;
    private boolean systemInfo;

    private Map<String, ProviderRepresentation> providers;

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public boolean isSystemInfo() {
        return systemInfo;
    }

    public void setSystemInfo(boolean systemInfo) {
        this.systemInfo = systemInfo;
    }

    public Map<String, ProviderRepresentation> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderRepresentation> providers) {
        this.providers = providers;
    }

}
