package org.keycloak.services.managers;

import org.keycloak.services.models.RealmManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RequiredCredentialModel;
import org.keycloak.services.resources.RegistrationService;
import org.picketlink.idm.model.Realm;
import org.picketlink.idm.model.SimpleRole;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class InstallationManager {
    public void install(RealmManager manager) {
        RealmModel defaultRealm = manager.createRealm(Realm.DEFAULT_REALM, Realm.DEFAULT_REALM);
        defaultRealm.setName(Realm.DEFAULT_REALM);
        defaultRealm.setEnabled(true);
        defaultRealm.setTokenLifespan(300);
        defaultRealm.setAccessCodeLifespan(60);
        defaultRealm.setSslNotRequired(false);
        defaultRealm.setCookieLoginAllowed(true);
        manager.generateRealmKeys(defaultRealm);
        defaultRealm.updateRealm();
        defaultRealm.addRequiredCredential(RequiredCredentialModel.PASSWORD);
        defaultRealm.getIdm().add(new SimpleRole(RegistrationService.REALM_CREATOR_ROLE));

    }
}
