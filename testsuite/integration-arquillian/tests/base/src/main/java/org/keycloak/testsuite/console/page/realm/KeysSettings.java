package org.keycloak.testsuite.console.page.realm;

/**
 *
 * @author tkyjovsk
 */
public class KeysSettings extends RealmSettings {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/cache-settings";
    }

}
