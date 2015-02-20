package org.keycloak.federation.kerberos;

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
        String editModeString = getConfig().get("editMode");
        if (editModeString == null) {
            return UserFederationProvider.EditMode.UNSYNCED;
        } else {
            return UserFederationProvider.EditMode.valueOf(editModeString);
        }
    }

    public boolean isAllowPasswordAuthentication() {
        return Boolean.valueOf(getConfig().get("allowPasswordAuthentication"));
    }

    public boolean isUpdateProfileFirstLogin() {
        return Boolean.valueOf(getConfig().get("updateProfileFirstLogin"));
    }

}
