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

package org.keycloak.marshalling;

import org.infinispan.protostream.descriptors.FileDescriptor;
import org.junit.Assert;
import org.junit.Test;

public class IndexSchemaChangeTest {

    private static final FileDescriptor V1 = KeycloakModelSchema.parseProtoSchema(TestModelV1.INSTANCE.getProtoFile());
    private static final FileDescriptor V2 = KeycloakModelSchema.parseProtoSchema(TestModelV2.INSTANCE.getProtoFile());

    @Test
    public void testNothingChanged() {
        doTest("keycloak.test.NothingChangedClass", false);
    }

    @Test
    public void testNothingChangedIndexed() {
        doTest("keycloak.test.NothingChangedIndexClass", false);
    }

    @Test
    public void testAddIndexedField() {
        doTest("keycloak.test.AddIndexedFieldClass", true);
    }

    @Test
    public void testRemoveIndexedField() {
        doTest("keycloak.test.RemoveIndexedFieldClass", true);
    }

    @Test
    public void testChangedIndexedFieldAttribute() {
        doTest("keycloak.test.ChangedIndexedFieldAttributeClass", true);
    }

    @Test
    public void testChangedIndexedField() {
        doTest("keycloak.test.ChangedIndexedFieldClass", true);
    }

    private static void doTest(String entity, boolean expectChanged) {
        var v1 = KeycloakModelSchema.findEntity(V1, entity);
        var v2 = KeycloakModelSchema.findEntity(V2, entity);
        Assert.assertTrue(v1.isPresent());
        Assert.assertTrue(v2.isPresent());
        Assert.assertEquals(expectChanged, KeycloakIndexSchemaUtil.isIndexSchemaChanged(v1.get(), v2.get()));
    }
}
