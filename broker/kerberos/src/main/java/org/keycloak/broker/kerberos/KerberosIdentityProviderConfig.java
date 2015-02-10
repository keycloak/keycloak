package org.keycloak.broker.kerberos;

import org.keycloak.models.IdentityProviderModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosIdentityProviderConfig extends IdentityProviderModel {

    public KerberosIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
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

}
