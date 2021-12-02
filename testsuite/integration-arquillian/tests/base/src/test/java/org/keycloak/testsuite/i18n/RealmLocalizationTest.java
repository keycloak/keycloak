package org.keycloak.testsuite.i18n;

import org.junit.Test;
import org.keycloak.admin.client.resource.RealmLocalizationResource;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;

public class RealmLocalizationTest extends AbstractI18NTest {

    /**
     * Make sure that realm localization texts support unicode ().
     */
    @Test
    public void realmLocalizationTextsSupportUnicode() {
        String locale = "en";
        String key = "Äǜṳǚǘǖ";
        String text = "Öṏṏ";
        RealmLocalizationResource localizationResource = testRealm().localization();
        localizationResource.saveRealmLocalizationText(locale, key, text);

        Map<String, String> localizationTexts = localizationResource.getRealmLocalizationTexts(locale);

        assertThat(localizationTexts, hasEntry(key, text));
    }

}
