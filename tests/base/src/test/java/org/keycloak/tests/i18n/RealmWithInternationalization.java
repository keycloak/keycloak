package org.keycloak.tests.i18n;

import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;

public class RealmWithInternationalization implements RealmConfig {

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        return realm.resetPasswordAllowed(true).internationalizationEnabled(true).supportedLocales("de", "en");
    }

}
