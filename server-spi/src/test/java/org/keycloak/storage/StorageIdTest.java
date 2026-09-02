/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.storage;

import org.keycloak.component.ComponentModel;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author hmlnarik
 */
public class StorageIdTest {

    @Test
    public void testStatic() {
        final String localId = "123";
        assertThat(StorageId.externalId(localId), is("123"));
        assertThat(StorageId.providerId(localId), nullValue());
        assertTrue(StorageId.isLocalStorage(localId));

        final String remoteId = "f:abc:123";
        assertThat(StorageId.externalId(remoteId), is("123"));
        assertThat(StorageId.providerId(remoteId), is("abc"));
        assertFalse(StorageId.isLocalStorage(remoteId));

        final ComponentModel cm = new ComponentModel();
        cm.setId("localId");
        assertThat(StorageId.keycloakId(cm, localId), is("f:localId:123"));
    }

    @Test
    public void testLocalId() {
        StorageId id = new StorageId("123");
        assertThat(id, notNullValue());
        assertThat(id.getExternalId(), is("123"));
        assertThat(id.getProviderId(), nullValue());
        assertThat(id.getId(), is("123"));
        assertTrue(id.isLocal());
    }

    @Test
    public void testExternalIdString() {
        StorageId id = new StorageId("f:abc:123");
        assertThat(id, notNullValue());
        assertThat(id.getExternalId(), is("123"));
        assertThat(id.getProviderId(), is("abc"));
        assertThat(id.getId(), is("f:abc:123"));
        assertFalse(id.isLocal());
    }

    @Test
    public void testExternalIdTwoStrings() {
        StorageId id = new StorageId("abc", "123");
        assertThat(id, notNullValue());
        assertThat(id.getExternalId(), is("123"));
        assertThat(id.getProviderId(), is("abc"));
        assertThat(id.getId(), is("f:abc:123"));
        assertFalse(id.isLocal());
    }

    @Test
    public void testEquals() {
        assertThat(new StorageId("abc", "123"), equalTo(new StorageId("f:abc:123")));
        assertThat(new StorageId("abc", "123"), not(equalTo(new StorageId("f:abc:1234"))));
    }
}
