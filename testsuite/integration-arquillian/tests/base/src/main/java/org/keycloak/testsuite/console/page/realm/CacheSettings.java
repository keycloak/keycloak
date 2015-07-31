package org.keycloak.testsuite.console.page.realm;

/**
 *
 * @author tkyjovsk
 */
public class CacheSettings extends RealmSettings {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/cache-settings";
    }

}
