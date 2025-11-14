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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.keycloak.models.jpa.entities.UserAttributeEntity;
import org.keycloak.models.jpa.entities.UserEntity;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

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

    @Test
    public void testPredicateForFilteringUsersByAttributes() {
        UserEntity user = new UserEntity();
        user.setAttributes(List.of(createAttribute("key1", "value1"), createAttribute("key2", "Value2")));

        assertThat(JpaHashUtils.predicateForFilteringUsersByAttributes(Map.of("key1", "value1", "key2", "Value2"), JpaHashUtils::compareSourceValue).test(user), is(true));
        assertThat(JpaHashUtils.predicateForFilteringUsersByAttributes(Map.of("key1", "value1", "key2", "value2"), JpaHashUtils::compareSourceValue).test(user), is(false));
        assertThat(JpaHashUtils.predicateForFilteringUsersByAttributes(Map.of("key1", "value1"), JpaHashUtils::compareSourceValue).test(user), is(true));
        assertThat(JpaHashUtils.predicateForFilteringUsersByAttributes(Map.of("key1", "value2"), JpaHashUtils::compareSourceValue).test(user), is(false));
        assertThat(JpaHashUtils.predicateForFilteringUsersByAttributes(Map.of("key2", "value1"), JpaHashUtils::compareSourceValue).test(user), is(false));
        assertThat(JpaHashUtils.predicateForFilteringUsersByAttributes(Map.of("key1", "v1"), JpaHashUtils::compareSourceValue).test(user), is(false));

        assertThat(JpaHashUtils.predicateForFilteringUsersByAttributes(Map.of("key1", "value1", "key2", "Value2"), JpaHashUtils::compareSourceValueLowerCase).test(user), is(true));
        assertThat(JpaHashUtils.predicateForFilteringUsersByAttributes(Map.of("key1", "value1", "key2", "value2"), JpaHashUtils::compareSourceValueLowerCase).test(user), is(true));
        assertThat(JpaHashUtils.predicateForFilteringUsersByAttributes(Map.of("key1", "value1"), JpaHashUtils::compareSourceValueLowerCase).test(user), is(true));
        assertThat(JpaHashUtils.predicateForFilteringUsersByAttributes(Map.of("key1", "value2"), JpaHashUtils::compareSourceValueLowerCase).test(user), is(false));
        assertThat(JpaHashUtils.predicateForFilteringUsersByAttributes(Map.of("key2", "value1"), JpaHashUtils::compareSourceValueLowerCase).test(user), is(false));
        assertThat(JpaHashUtils.predicateForFilteringUsersByAttributes(Map.of("key1", "v1"), JpaHashUtils::compareSourceValueLowerCase).test(user), is(false));
    }

    private UserAttributeEntity createAttribute(String key, String value) {
        UserAttributeEntity attribute = new UserAttributeEntity();
        attribute.setName(key);
        attribute.setValue(value);
        return attribute;
    }
}