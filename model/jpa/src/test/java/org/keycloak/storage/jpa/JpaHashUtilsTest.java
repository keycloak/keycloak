/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.storage.jpa;

import org.junit.Test;

import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link JpaHashUtils}.
 * @author Alexander Schwartz
 */
public class JpaHashUtilsTest {

    @Test
    public void differentCaseShouldCreateDifferentHash() {
        byte[] hash1 = JpaHashUtils.hashForAttributeValue("a");
        byte[] hash2 = JpaHashUtils.hashForAttributeValue("A");
        assertThat(hash1, not(equalTo(hash2)));
    }

    @Test
    public void differentCaseShouldCreateSameHashForLowercase() {
        byte[] hash1 = JpaHashUtils.hashForAttributeValueLowerCase("a");
        byte[] hash2 = JpaHashUtils.hashForAttributeValueLowerCase("A");
        assertThat(hash1, equalTo(hash2));
    }

    /**
     * This shows that the default english locale used here correctly handles German umlauts as expected.
     */
    @Test
    public void differentCaseShouldCreateSameHashForLowercaseGermanCharacters() {
        byte[] hash1 = JpaHashUtils.hashForAttributeValueLowerCase("\u00E4");
        byte[] hash2 = JpaHashUtils.hashForAttributeValueLowerCase("\u00C4");
        assertThat(hash1, equalTo(hash2));
    }

    /**
     * Although a caller in a turkish context might expect this to work, it won't as we enforce a hard-coded locale
     * to avoid the need to re-hash on changes in the runtime locale.
     */
    @Test
    public void differentCaseShouldCreateSameHashForLowercaseTurkishI() {
        byte[] hash1 = JpaHashUtils.hashForAttributeValueLowerCase("I");
        byte[] hash2 = JpaHashUtils.hashForAttributeValueLowerCase("I".toLowerCase(new Locale("tr")));
        assertThat(hash1, not(equalTo(hash2)));
    }

}