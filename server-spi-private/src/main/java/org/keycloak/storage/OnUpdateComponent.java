package org.keycloak.storage;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * Callback for component update.  Only hardcoded classes like UserStorageManager implement it.  In future we
 * may allow anybody to implement this interface.
 *
 * @author <a href="mailto:froehlich.ch@gmail.com">Christian Froehlich</a>
 * @version $Revision: 1 $
 */
public interface OnUpdateComponent {
    void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel model);
}
