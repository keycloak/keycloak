package org.keycloak.federation.kerberos;

import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;

/**
 * Configuration specific to {@link KerberosFederationProvider}
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosConfig extends CommonKerberosConfig {

    public KerberosConfig(UserFederationProviderModel userFederationProvider) {
        super(userFederationProvider);
    }

    public UserFederationProvider.EditMode getEditMode() {
        String editModeString = getConfig().get(LDAPConstants.EDIT_MODE);
        if (editModeString == null) {
            return UserFederationProvider.EditMode.UNSYNCED;
        } else {
            return UserFederationProvider.EditMode.valueOf(editModeString);
        }
    }

    public boolean isAllowPasswordAuthentication() {
        return Boolean.valueOf(getConfig().get(KerberosConstants.ALLOW_PASSWORD_AUTHENTICATION));
    }

    public boolean isUpdateProfileFirstLogin() {
        return Boolean.valueOf(getConfig().get(KerberosConstants.UPDATE_PROFILE_FIRST_LOGIN));
    }

}
