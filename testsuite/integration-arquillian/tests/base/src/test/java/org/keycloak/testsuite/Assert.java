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

package org.keycloak.testsuite;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserFederationProviderFactoryRepresentation;
import org.keycloak.representations.idm.UserProfileAttributeMetadata;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.info.ThemeInfoRepresentation;

import org.hamcrest.MatcherAssert;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Assert extends org.junit.Assert {

    public static final Long DEFAULT_NUMBER_DEVIATION = 20L;

    public static <T> void assertNames(Set<T> actual, String... expected) {
        Arrays.sort(expected);
        String[] actualNames = names(new LinkedList<Object>(actual));
        assertArrayEquals("Expected: " + Arrays.toString(expected) + ", was: " + Arrays.toString(actualNames), expected, actualNames);
    }

    public static <T> void assertNames(List<T> actual, String... expected) {
        Arrays.sort(expected);
        String[] actualNames = names(actual);
        assertArrayEquals("Expected: " + Arrays.toString(expected) + ", was: " + Arrays.toString(actualNames), expected, actualNames);
    }

    private static <T> String[] names(List<T> list) {
        String[] names = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            names[i] = name(list.get(i));
        }
        Arrays.sort(names);
        return names;
    }

    private static String name(Object o1) {
        if (o1 instanceof String) {
            return (String) o1;
        } else if (o1 instanceof RealmRepresentation) {
            return ((RealmRepresentation) o1).getRealm();
        } else if (o1 instanceof ClientRepresentation) {
            return ((ClientRepresentation) o1).getClientId();
        } else if (o1 instanceof IdentityProviderRepresentation) {
            return ((IdentityProviderRepresentation) o1).getAlias();
        } else if (o1 instanceof RoleRepresentation) {
            return ((RoleRepresentation) o1).getName();
        } else if (o1 instanceof UserRepresentation) {
            return ((UserRepresentation) o1).getUsername();
        } else if (o1 instanceof UserFederationProviderFactoryRepresentation) {
            return ((UserFederationProviderFactoryRepresentation) o1).getId();
        } else if (o1 instanceof GroupRepresentation) {
            return ((GroupRepresentation) o1).getName();
        } else if (o1 instanceof ComponentRepresentation) {
            return ((ComponentRepresentation) o1).getName();
        } else if (o1 instanceof ClientScopeRepresentation) {
            return ((ClientScopeRepresentation) o1).getName();
        } else if (o1 instanceof ThemeInfoRepresentation) {
            return ((ThemeInfoRepresentation) o1).getName();
        } else if (o1 instanceof UserProfileAttributeMetadata) {
            return ((UserProfileAttributeMetadata) o1).getName();
        }

        throw new IllegalArgumentException();
    }


    /**
     * Assert all the fields from map available. Array "expected" contains pairs when first value from pair is expected key
     * and second is the expected value from the map for target key.
     *
     * Example config = {"key1" -> "value1" , "key2" -> "value2" }
     * then assertMap(config, "key1", "value1", "key2", "value2" will return true
     *
     */
    public static void assertMap(Map<String, String> config, String... expected) {
        if (expected == null) {
            expected = new String[] {};
        }

        Assert.assertEquals(config.size() * 2, expected.length);
        for (int i=0 ; i<expected.length ; i+=2) {
            String key = expected[i];
            String value = expected[i+1];
            Assert.assertEquals(value, config.get(key));
        }
    }

    /**
     * Assert all the fields from map available. Array "expected" contains pairs when first value from pair is expected key
     * and second is the expected value from the map for target key.
     *
     * Example config = {"key1" -> "value1" , "key2" -> "value2" }
     * then assertMap(config, "key1", "value1", "key2", "value2" will return true
     *
     */
    public static void assertMultivaluedMap(MultivaluedHashMap<String, String> config, String... expected) {
        if (expected == null) {
            expected = new String[] {};
        }

        for (int i=0 ; i<expected.length ; i+=2) {
            String key = expected[i];
            String value = expected[i+1];
            Assert.assertEquals(value, config.getFirst(key));
        }
    }

    public static void assertProviderConfigProperty(ConfigPropertyRepresentation property, String name, String label, String defaultValue, String helpText, String type) {
        Assert.assertEquals(name, property.getName());
        Assert.assertEquals(label, property.getLabel());
        Assert.assertEquals(defaultValue, property.getDefaultValue());
        Assert.assertEquals(helpText, property.getHelpText());
        Assert.assertEquals(type, property.getType());
    }

    public static void assertExpiration(int actual, int expected) {
        assertExpiration((long) actual, (long) expected);
    }

    public static void assertExpiration(long actual, long expected) {
        assertExpiration(actual, expected, DEFAULT_NUMBER_DEVIATION);
    }

    public static void assertExpiration(long actual, long expected, long deviation) {
        MatcherAssert.assertThat(actual, allOf(greaterThanOrEqualTo(expected - deviation), lessThanOrEqualTo(expected + deviation)));
    }

    public static void assertRoleAttributes(Map<String, List<String>> expected, Map<String, List<String>> actual) {
        MatcherAssert.assertThat(actual.keySet(), equalTo(expected.keySet()));
        for (var entry : expected.entrySet()) {
            String expectedKey = entry.getKey();
            MatcherAssert.assertThat(actual.get(expectedKey).size(), is(equalTo(entry.getValue().size())));
            MatcherAssert.assertThat(actual.get(expectedKey), containsInAnyOrder(entry.getValue().toArray()));
        }
    }
}
