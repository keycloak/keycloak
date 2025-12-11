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

package org.keycloak.storage.jpa;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

import org.keycloak.crypto.HashException;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.models.jpa.entities.UserEntity;

/**
 * Create hashes for long values stored in the database. Offers different variants for exact and lowercase search.
 * Keycloak uses lowercase search to approximate a case-insensitive search.
 * <p>
 * The lowercase function always uses the English locale to avoid changing hashes due to changing locales which can be surprising
 * and would be expensive to fix as all hashes would need to be re-calculated.
 *
 * @author Alexander Schwartz
 */
public class JpaHashUtils {

    private static byte[] hash(byte[] inputBytes) {
        try {
            MessageDigest md = MessageDigest.getInstance(JavaAlgorithm.SHA512);
            md.update(inputBytes);
            return md.digest();
        } catch (Exception e) {
            throw new HashException("Error when creating token hash", e);
        }
    }

    public static byte[] hashForAttributeValue(String value) {
        return JpaHashUtils.hash(value.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] hashForAttributeValueLowerCase(String value) {
        return JpaHashUtils.hash(value.toLowerCase(Locale.ENGLISH).getBytes(StandardCharsets.UTF_8));
    }

    public static boolean compareSourceValueLowerCase(String value1, String value2) {
        return Objects.equals(value1.toLowerCase(Locale.ENGLISH), value2.toLowerCase(Locale.ENGLISH));
    }

    public static boolean compareSourceValue(String value1, String value2) {
        return Objects.equals(value1, value2);
    }

    /**
     * This method returns a predicate that returns true when user has all attributes specified in {@code customLongValueSearchAttributes} map
     * <p />
     * The check is performed by exact comparison on attribute name the value
     * <p />
     * This is necessary because database can return users without the searched attribute when a hash collision on long user attribute value occurs
     *
     * @param customLongValueSearchAttributes required attributes
     * @param valueComparator                 comparator for comparing attribute values
     * @return predicate for filtering users based on attributes map
     */
    public static java.util.function.Predicate<UserEntity> predicateForFilteringUsersByAttributes(Map<String, String> customLongValueSearchAttributes, BiPredicate<String, String> valueComparator) {
        return userEntity -> customLongValueSearchAttributes.isEmpty() || // are there some long attribute values
                customLongValueSearchAttributes
                        .entrySet()
                        .stream()
                        .allMatch(longAttrEntry -> //for all long search attributes
                                userEntity
                                        .getAttributes()
                                        .stream()
                                        .anyMatch(userAttribute -> //check whether the user indeed has the attribute
                                                Objects.equals(longAttrEntry.getKey().toLowerCase(), userAttribute.getName().toLowerCase())
                                                        && valueComparator.test(longAttrEntry.getValue(), userAttribute.getValue())
                                        )
                        );
    }

}
