package org.keycloak.test;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RequiredCredentialModel;
import org.keycloak.services.resources.SaasService;
import org.keycloak.services.resources.SaasService;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class InstallationManager {
    public void install(RealmManager manager) {
        RealmModel defaultRealm = manager.createRealm(RealmModel.DEFAULT_REALM, RealmModel.DEFAULT_REALM);
        defaultRealm.setName(RealmModel.DEFAULT_REALM);
        defaultRealm.setEnabled(true);
        defaultRealm.setTokenLifespan(300);
        defaultRealm.setAccessCodeLifespan(60);
        defaultRealm.setSslNotRequired(false);
        defaultRealm.setCookieLoginAllowed(true);
        defaultRealm.setRegistrationAllowed(true);
        manager.generateRealmKeys(defaultRealm);
        defaultRealm.addRequiredCredential(CredentialRepresentation.PASSWORD);
        defaultRealm.addRole(SaasService.REALM_CREATOR_ROLE);
    }

    public boolean isInstalled(RealmManager manager) {
        return manager.defaultRealm() != null;
    }
}
