package org.keycloak.tests.i18n;

import java.util.Map;

import org.keycloak.admin.client.resource.RealmLocalizationResource;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;

@KeycloakIntegrationTest
public class RealmLocalizationTest {

    @InjectRealm(config = RealmWithInternationalization.class)
    ManagedRealm managedRealm;

    /**
     * Make sure that realm localization texts support unicode ().
     */
    @Test
    public void realmLocalizationTextsSupportUnicode() {
        String locale = "en";
        String key = "Äǜṳǚǘǖ";
        String text = "Öṏṏ";
        RealmLocalizationResource localizationResource = managedRealm.admin().localization();
        localizationResource.saveRealmLocalizationText(locale, key, text);

        Map<String, String> localizationTexts = localizationResource.getRealmLocalizationTexts(locale, false);

        assertThat(localizationTexts, hasEntry(key, text));
    }

}
