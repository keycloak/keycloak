package org.keycloak.federation.ldap.kerberos;

import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.models.UserFederationProviderModel;

/**
 * Configuration specific to {@link org.keycloak.federation.ldap.LDAPFederationProvider}
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPProviderKerberosConfig extends CommonKerberosConfig {

    public LDAPProviderKerberosConfig(UserFederationProviderModel userFederationProvider) {
        super(userFederationProvider);
    }

    public boolean isUseKerberosForPasswordAuthentication() {
        return Boolean.valueOf(getConfig().get(KerberosConstants.USE_KERBEROS_FOR_PASSWORD_AUTHENTICATION));
    }
}
