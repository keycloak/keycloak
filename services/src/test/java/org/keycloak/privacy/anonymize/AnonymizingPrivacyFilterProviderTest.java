package org.keycloak.privacy.anonymize;

import org.junit.Test;
import org.keycloak.privacy.PrivacyFilterProvider;
import org.keycloak.privacy.PrivacyTypeHints;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AnonymizingPrivacyFilterProviderTest {

    /**
     * see KEYCLOAK-13160
     */
    @Test
    public void filteringWithDefaults() {

        PrivacyFilterProvider provider = AnonymizingPrivacyFilterProviderFactoryTest.createProvider(null);

        assertEquals("7a%b5f", provider.filter("7adf8d60-8205-44d3-a191-6cd5e22d7b5f", PrivacyTypeHints.USER_ID));
        assertEquals("te%ter", provider.filter("tester", PrivacyTypeHints.USERNAME));
        assertEquals("th%com", provider.filter("thomas.darimont@example.com", PrivacyTypeHints.EMAIL));
        assertEquals("Th%ont", provider.filter("Thomas Darimont", PrivacyTypeHints.NAME));
        assertEquals("19%112", provider.filter("192.168.99.112", PrivacyTypeHints.IP_ADDRESS));
        assertEquals("so%ata", provider.filter("some personal data", PrivacyTypeHints.PERSONAL_DATA));
    }

    /**
     * see KEYCLOAK-13160
     */
    @Test
    public void filterPlain() {

        // Don't filter userIds
        Map<String, String> config = Collections.singletonMap("typeHintAliasMapping", "userId:plain");

        PrivacyFilterProvider provider = AnonymizingPrivacyFilterProviderFactoryTest.createProvider(config);

        assertEquals("th%com", provider.filter("thomas.darimont@example.com", PrivacyTypeHints.EMAIL));
        assertEquals("7adf8d60-8205-44d3-a191-6cd5e22d7b5f", provider.filter("7adf8d60-8205-44d3-a191-6cd5e22d7b5f", PrivacyTypeHints.USER_ID));
    }
}