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

package org.keycloak.tests.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest
public class RealmLocalizationResourceTest {

    @InjectRealm(config = RealmLocaleConfig.class)
    private ManagedRealm realm;

    private static class RealmLocaleConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            RealmRepresentation rep = realm.build();
            rep.setDefaultLocale("en");
            rep.setLocalizationTexts(Map.of("en", Map.of("key-a", "text-a_en", "key-b", "text-b_en"), "de", Map.of("key-a", "text-a_de")));
            rep.setEnabled(true);

            return realm.update(rep);
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
        realm.cleanup().add(r -> r.localization().deleteRealmLocalizationText("en", "key-c"));

        realm.admin().localization().saveRealmLocalizationText("en", "key-c", "text-c");

        String localizationText = realm.admin().localization().getRealmLocalizationText("en", "key-c");

        assertNotNull(localizationText);
        assertEquals("text-c", localizationText);
    }

    @Test
    public void updateRealmLocalizationText() {
        realm.cleanup().add(r -> r.localization().saveRealmLocalizationText("en", "key-b", "text-b_en"));

        realm.admin().localization().saveRealmLocalizationText("en", "key-b", "text-b-new");

        String localizationText = realm.admin().localization().getRealmLocalizationText("en", "key-b");

        assertNotNull(localizationText);
        assertEquals("text-b-new", localizationText);
    }

    @Test
    public void deleteRealmLocalizationText() {
        realm.cleanup().add(r -> r.localization().saveRealmLocalizationText("en", "key-a", "text-a_en"));

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
        realm.cleanup().add(r -> r.localization().saveRealmLocalizationText("en", "key-a", "text-a_en"));
        realm.cleanup().add(r -> r.localization().saveRealmLocalizationText("en", "key-b", "text-b_en"));

        realm.admin().localization().deleteRealmLocalizationTexts("en");
        
        List<String> localizations = realm.admin().localization().getRealmSpecificLocales();
        assertEquals(1, localizations.size());

        assertThat(localizations, CoreMatchers.hasItems("de"));
    }

    @Test
    public void createOrUpdateRealmLocalizationWhenLocaleDoesNotYetExist() {
        realm.cleanup().add(r -> r.localization().deleteRealmLocalizationTexts("es"));

        final Map<String, String> newLocalizationTexts = new HashMap<>();
        newLocalizationTexts.put("key-a", "text-a_es");
        newLocalizationTexts.put("key-b", "text-b_es");

        realm.admin().localization().createOrUpdateRealmLocalizationTexts("es", newLocalizationTexts);

        final Map<String, String> persistedLocalizationTexts = realm.admin().localization().getRealmLocalizationTexts("es");
        assertEquals(newLocalizationTexts, persistedLocalizationTexts);
    }

    @Test
    public void createOrUpdateRealmLocalizationWhenLocaleAlreadyExists() {
        realm.cleanup().add(r -> r.localization().saveRealmLocalizationText("en", "key-b", "text-b_en"));

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
