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
import org.keycloak.privacy.PrivacyTypeHints;

import static org.junit.Assert.assertEquals;

public class DefaultAnonymizerTest {

    /**
     * see KEYCLOAK-13160
     */
    @Test
    public void anonymizeByReplacingValue() {

        Anonymizer anonymizer = new DefaultAnonymizer(0, 0, 0, "%");
        String anonymized = anonymizer.anonymize("192.168.99.100", PrivacyTypeHints.IP_ADDRESS);
        assertEquals("%", anonymized);
    }

    /**
     * see KEYCLOAK-13160
     */
    @Test
    public void anonymizeByShorteningValueIPAddress() {

        Anonymizer anonymizer = new DefaultAnonymizer(5, 2, 3, "%");
        String anonymized = anonymizer.anonymize("192.168.99.100", PrivacyTypeHints.IP_ADDRESS);
        assertEquals("19%100", anonymized);
    }

    /**
     * see KEYCLOAK-13160
     */
    @Test
    public void anonymizeByShorteningValueEmail() {

        Anonymizer anonymizer = new DefaultAnonymizer(5, 2, 3, "%");
        String anonymized = anonymizer.anonymize("thomas.darimont@example.com", PrivacyTypeHints.EMAIL);
        assertEquals("th%com", anonymized);
    }
}