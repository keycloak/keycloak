package org.keycloak.federation.kerberos;

import java.util.Map;

import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.utils.KerberosConstants;

/**
 * Common configuration useful for all providers
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class CommonKerberosConfig {

    private final UserFederationProviderModel providerModel;

    public CommonKerberosConfig(UserFederationProviderModel userFederationProvider) {
        this.providerModel = userFederationProvider;
    }

    // Should be always true for KerberosFederationProvider
    public boolean isAllowKerberosAuthentication() {
        return Boolean.valueOf(getConfig().get(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION));
    }

    public String getKerberosRealm() {
        return getConfig().get("kerberosRealm");
    }

    public String getServerPrincipal() {
        return getConfig().get("serverPrincipal");
    }

    public String getKeyTab() {
        return getConfig().get("keyTab");
    }

    public boolean getDebug() {
        return Boolean.valueOf(getConfig().get("debug"));
    }

    protected Map<String, String> getConfig() {
        return providerModel.getConfig();
    }

}
