/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.realm;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;

public class RealmEntityUndefinedValuesTest {

    public MapRealmEntity newMapRealmEntity() {
        return new MapRealmEntityImpl();
    }

    @Test
    public void testUndefinedValuesToCollection() {
        // setup
        MapRealmEntity realmEntity = newMapRealmEntity();
        {
            // when
            realmEntity.setEventsListeners(null);

            // then
            assertThat(realmEntity.getEventsListeners(), nullValue());
        }
        {
            // when
            realmEntity.setEventsListeners(Collections.emptySet());

            // then
            assertThat(realmEntity.getEventsListeners(), nullValue());
        }
        {
            // when
            realmEntity.setEventsListeners(Collections.singleton(null));

            // then
            assertThat(realmEntity.getEventsListeners(), nullValue());
        }
        {
            // when
            realmEntity.setEventsListeners(Collections.singleton("listener1"));

            // then
            assertThat(realmEntity.getEventsListeners(), contains("listener1"));
        }
        {
            // when
            realmEntity.setEventsListeners(Collections.emptySet());

            // then
            assertThat(realmEntity.getEventsListeners(), nullValue());
        }
        {
            // when
            realmEntity.addOptionalClientScopeId(null);

            // then
            assertThat(realmEntity.getOptionalClientScopeIds(), nullValue());
        }
        {
            // when
            realmEntity.addOptionalClientScopeId("id1");

            // then
            assertThat(realmEntity.getOptionalClientScopeIds(), notNullValue());
            assertThat(realmEntity.getOptionalClientScopeIds(), contains("id1"));
        }
        {
            // when
            realmEntity.addOptionalClientScopeId(null);

            // then
            assertThat(realmEntity.getOptionalClientScopeIds(), notNullValue());
            assertThat(realmEntity.getOptionalClientScopeIds(), contains("id1"));
        }
    }

    @Test
    public void testAddUndefinedValuesToMapStringString() {
        // setup
        MapRealmEntity realmEntity = newMapRealmEntity();
        Map<String, String> headers = new HashMap<>();

        {
            // when
            realmEntity.setBrowserSecurityHeaders(Collections.emptyMap());

            // then
            assertThat(realmEntity.getBrowserSecurityHeaders(), nullValue());
        }
        {
            // when
            headers.put("key1", null);
            realmEntity.setBrowserSecurityHeaders(headers);

            // then
            assertThat(realmEntity.getBrowserSecurityHeaders(), nullValue());
        }
        {
            // when
            headers.put("key1", "value1");
            realmEntity.setBrowserSecurityHeaders(headers);

            // then
            assertThat(realmEntity.getBrowserSecurityHeaders(), notNullValue());
            assertThat(realmEntity.getBrowserSecurityHeaders(), allOf(aMapWithSize(1), hasEntry(equalTo("key1"), equalTo("value1"))));
        }
        {
            // when
            headers.put("key2", null);
            realmEntity.setBrowserSecurityHeaders(headers);

            // then
            assertThat(realmEntity.getBrowserSecurityHeaders(), notNullValue());
            assertThat(realmEntity.getBrowserSecurityHeaders(), allOf(aMapWithSize(1), hasEntry(equalTo("key1"), equalTo("value1"))));
        }
        {
            // when
            realmEntity.setBrowserSecurityHeaders(Collections.emptyMap());

            // then
            assertThat(realmEntity.getBrowserSecurityHeaders(), nullValue());
        }
        {
            // when
            realmEntity.setBrowserSecurityHeader("key1", null);

            // then
            assertThat(realmEntity.getBrowserSecurityHeaders(), nullValue());
        }
        {
            // when
            realmEntity.setBrowserSecurityHeader("key1", "value1");

            // then
            assertThat(realmEntity.getBrowserSecurityHeaders(), allOf(aMapWithSize(1), hasEntry(equalTo("key1"), equalTo("value1"))));
        }
        {
            // when
            realmEntity.setBrowserSecurityHeader("key2", null);

            // then
            assertThat(realmEntity.getBrowserSecurityHeaders(), allOf(aMapWithSize(1), hasEntry(equalTo("key1"), equalTo("value1"))));
        }
        {
            // when
            realmEntity.setBrowserSecurityHeader("key1", null);

            // then
            // TODO: Should we set map to null if we remove last entry by calling set*(key, null) method?
            assertThat(realmEntity.getBrowserSecurityHeaders(), anEmptyMap());
        }
    }

    @Test
    public void testAddUndefinedValuesToMapStringList() {
        MapRealmEntity realmEntity = newMapRealmEntity();
        Map<String, List<String>> attributes = new HashMap<>();

        {
            // when
            attributes.put("key1", Collections.emptyList());
            realmEntity.setAttributes(attributes);

            // then
            assertThat(realmEntity.getAttributes(), nullValue());
        }
        {
            // when
            attributes.put("key1", Collections.singletonList(null));
            realmEntity.setAttributes(attributes);

            // then
            assertThat(realmEntity.getAttributes(), nullValue());
        }
        {
            // when
            attributes.put("key1", Arrays.asList(null, null, null));
            realmEntity.setAttributes(attributes);

            // then
            assertThat(realmEntity.getAttributes(), nullValue());
        }
    }

    @Test
    public void testAddUndefinedValuesToMapStringMap() {
        MapRealmEntity realmEntity = newMapRealmEntity();
        Map<String, String> localizationTexts = new HashMap<>();

        {
            // when
            realmEntity.setLocalizationText("en", Collections.emptyMap());

            // then
            assertThat(realmEntity.getLocalizationText("en"), nullValue());
            assertThat(realmEntity.getLocalizationTexts(), nullValue());
        }
        {
            // when
            realmEntity.setLocalizationText("en", Collections.singletonMap("key1", null));

            // then
            assertThat(realmEntity.getLocalizationText("en"), nullValue());
            assertThat(realmEntity.getLocalizationTexts(), nullValue());
        }
        {
            // when
            realmEntity.setLocalizationText("en", Collections.singletonMap("key1", "value1"));

            // then
            assertThat(realmEntity.getLocalizationTexts(), allOf(aMapWithSize(1), hasKey("en")));
            assertThat(realmEntity.getLocalizationText("en"), allOf(aMapWithSize(1), hasEntry(equalTo("key1"), equalTo("value1"))));
        }
        {
            // when
            localizationTexts.put("key1", "value1");
            localizationTexts.put("key2", null);
            realmEntity.setLocalizationText("en", localizationTexts);

            // then
            assertThat(realmEntity.getLocalizationTexts(), allOf(aMapWithSize(1), hasKey("en")));
            assertThat(realmEntity.getLocalizationText("en"), allOf(aMapWithSize(1), hasEntry(equalTo("key1"), equalTo("value1"))));
        }
        {
            // when
            localizationTexts.put("key1", null);
            localizationTexts.put("key2", null);
            realmEntity.setLocalizationText("en", localizationTexts);

            // then
            assertThat(realmEntity.getLocalizationTexts(), anEmptyMap());
            assertThat(realmEntity.getLocalizationText("en"), nullValue());
        }
    }
}
