package org.keycloak.tests.i18n;

import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

public class RealmWithInternationalization implements RealmConfig {

    @Override
    public RealmConfigBuilder configure(RealmConfigBuilder realm) {
        return realm.resetPasswordAllowed(true).internationalizationEnabled(true).supportedLocales("de", "en");
    }

}
