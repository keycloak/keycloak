/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.test.admin;

import jakarta.ws.rs.NotFoundException;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.test.framework.annotations.InjectRealm;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.realm.ManagedRealm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest
public class RealmLocalizationResourceTest {

    @InjectRealm
    private ManagedRealm realm;

    @BeforeEach
    public void setupRealmLocale() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setDefaultLocale("en");
        realm.admin().update(rep);

        realm.admin().localization().saveRealmLocalizationText("en", "key-a", "text-a_en");
        realm.admin().localization().saveRealmLocalizationText("en", "key-b", "text-b_en");
        realm.admin().localization().saveRealmLocalizationText("de", "key-a", "text-a_de");
    }

    @AfterEach
    public void cleanupRealmLocale() {
        if (realm.admin().localization().getRealmLocalizationTexts("en").size() > 0) {
            realm.cleanup().add(r -> r.localization().deleteRealmLocalizationTexts("en"));
        }
        if (realm.admin().localization().getRealmLocalizationTexts("de").size() > 0) {
            realm.cleanup().add(r -> r.localization().deleteRealmLocalizationTexts("de"));
        }
        if (realm.admin().localization().getRealmLocalizationTexts("es").size() > 0) {
            realm.cleanup().add(r -> r.localization().deleteRealmLocalizationTexts("es"));
        }
    }

    @Test
    public void getRealmSpecificLocales() {
        List<String> languages = realm.admin().localization().getRealmSpecificLocales();
        assertEquals(2, languages.size());
        assertThat(languages, CoreMatchers.hasItems("en", "de"));
    }

    @Test
    public void getRealmLocalizationTexts() {
        Map<String, String> localizations = realm.admin().localization().getRealmLocalizationTexts("en");
        assertNotNull(localizations);
        assertEquals(2, localizations.size());

        assertEquals("text-a_en", localizations.get("key-a"));
        assertEquals("text-b_en", localizations.get("key-b"));
    }

    @Test
    public void getRealmLocalizationTextsWithFallback() {
        Map<String, String> localizations = realm.admin().localization().getRealmLocalizationTexts("de", true);
        assertNotNull(localizations);
        assertEquals(2, localizations.size());

        assertEquals("text-a_de", localizations.get("key-a"));
        assertEquals("text-b_en", localizations.get("key-b"));
    }

    @Test
    public void getRealmLocalizationsNotExists() {
        Map<String, String> localizations = realm.admin().localization().getRealmLocalizationTexts("zz");
        assertNotNull(localizations);
        assertEquals(0, localizations.size());
    }

    @Test
    public void getRealmLocalizationText() {
        String localizationText = realm.admin().localization().getRealmLocalizationText("en", "key-a");
        assertNotNull(localizationText);
        assertEquals("text-a_en", localizationText);
    }

    @Test
    public void getRealmLocalizationTextNotExists() {
        assertThrows(NotFoundException.class, () -> {
            realm.admin().localization().getRealmLocalizationText("en", "key-zz");
        });
    }

    @Test
    public void addRealmLocalizationText() {
        realm.admin().localization().saveRealmLocalizationText("en", "key-c", "text-c");

        String localizationText = realm.admin().localization().getRealmLocalizationText("en", "key-c");

        assertNotNull(localizationText);
        assertEquals("text-c", localizationText);
    }

    @Test
    public void updateRealmLocalizationText() {
        realm.admin().localization().saveRealmLocalizationText("en", "key-b", "text-b-new");

        String localizationText = realm.admin().localization().getRealmLocalizationText("en", "key-b");

        assertNotNull(localizationText);
        assertEquals("text-b-new", localizationText);
    }

    @Test
    public void deleteRealmLocalizationText() {
        realm.admin().localization().deleteRealmLocalizationText("en", "key-a");

        Map<String, String> localizations = realm.admin().localization().getRealmLocalizationTexts("en");
        assertEquals(1, localizations.size());
        assertEquals("text-b_en", localizations.get("key-b"));
    }

    @Test
    public void deleteRealmLocalizationTextNotExists() {
        assertThrows(NotFoundException.class, () -> {
            realm.admin().localization().deleteRealmLocalizationText("en", "zz");
        });
    }

    @Test
    public void deleteRealmLocalizationTexts() {
        realm.admin().localization().deleteRealmLocalizationTexts("en");

        List<String> localizations = realm.admin().localization().getRealmSpecificLocales();
        assertEquals(1, localizations.size());

        assertThat(localizations, CoreMatchers.hasItems("de"));
    }

    @Test
    public void createOrUpdateRealmLocalizationWhenLocaleDoesNotYetExist() {
        final Map<String, String> newLocalizationTexts = new HashMap<>();
        newLocalizationTexts.put("key-a", "text-a_es");
        newLocalizationTexts.put("key-b", "text-b_es");

        realm.admin().localization().createOrUpdateRealmLocalizationTexts("es", newLocalizationTexts);

        final Map<String, String> persistedLocalizationTexts = realm.admin().localization().getRealmLocalizationTexts("es");
        assertEquals(newLocalizationTexts, persistedLocalizationTexts);
    }

    @Test
    public void createOrUpdateRealmLocalizationWhenLocaleAlreadyExists() {
        final Map<String, String> newLocalizationTexts = new HashMap<>();
        newLocalizationTexts.put("key-b", "text-b_changed_en");
        newLocalizationTexts.put("key-c", "text-c_en");

        realm.admin().localization().createOrUpdateRealmLocalizationTexts("en", newLocalizationTexts);

        final Map<String, String> expectedLocalizationTexts = new HashMap<>();
        expectedLocalizationTexts.put("key-a", "text-a_en");
        expectedLocalizationTexts.putAll(newLocalizationTexts);
        final Map<String, String> persistedLocalizationTexts = realm.admin().localization().getRealmLocalizationTexts("en");
        assertEquals(expectedLocalizationTexts, persistedLocalizationTexts);
    }
}
