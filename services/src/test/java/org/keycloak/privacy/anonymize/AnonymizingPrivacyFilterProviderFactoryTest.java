/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.privacy.anonymize;

import org.junit.Test;
import org.keycloak.Config;
import org.keycloak.privacy.PrivacyFilterProvider;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AnonymizingPrivacyFilterProviderFactoryTest {

    /**
     * see KEYCLOAK-13160
     */
    @Test
    public void createProviderWithDefaults() {

        Config.SystemPropertiesScope scope = new Config.SystemPropertiesScope("");
        AnonymizingPrivacyFilterProviderFactory factory = new AnonymizingPrivacyFilterProviderFactory();
        factory.init(scope);

        PrivacyFilterProvider provider = factory.create(null);

        String filtered = provider.filter("thomas.darimont@example.com", PrivacyFilterProvider.EMAIL);

        assertEquals("th%com", filtered);
    }

    /**
     * see KEYCLOAK-13160
     */
    @Test
    public void createProviderWithConfig() {

        Map<String, String> config = new HashMap<>();
        config.put("minLength", "5");
        config.put("prefixLength", "1");
        config.put("suffixLength", "2");
        config.put("placeHolder", "*");

        config.put("useDefaultFilteredTypeHints", "false");
        config.put("filteredTypeHints", "confidential,birthdate,pii");
        config.put("typeHintAliasMapping", "confidential:pii,birthdate:pii");
        config.put("fallbackTypeHint", PrivacyFilterProvider.PII);

        Config.SystemPropertiesScope scope = new Config.SystemPropertiesScope("") {
            @Override
            public String get(String key, String defaultValue) {
                return config.getOrDefault(key, defaultValue);
            }
        };

        AnonymizingPrivacyFilterProviderFactory factory = new AnonymizingPrivacyFilterProviderFactory();
        factory.init(scope);

        AnonymizingPrivacyFilterProvider provider = (AnonymizingPrivacyFilterProvider) factory.create(null);

        DefaultAnonymizer anonymizer = (DefaultAnonymizer) provider.getAnonymizer();
        assertEquals(5, anonymizer.getMinLength());
        assertEquals(1, anonymizer.getPrefixLength());
        assertEquals(2, anonymizer.getSuffixLength());
        assertEquals("*", anonymizer.getPlaceHolder());

        assertEquals(PrivacyFilterProvider.PII, provider.getFallbackTypeHint());
        assertEquals(PrivacyFilterProvider.PII, provider.getTypeAliases().get("birthdate"));
        assertTrue(provider.getTypeHints().contains("birthdate"));
        assertTrue(provider.getTypeHints().contains("pii"));

        assertEquals("thomas.darimont@example.com", provider.filter("thomas.darimont@example.com", PrivacyFilterProvider.EMAIL));
        assertEquals("t*om", provider.filter("thomas.darimont@example.com", "confidential"));
    }
}