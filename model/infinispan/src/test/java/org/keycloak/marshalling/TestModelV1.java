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

import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoSchema;
import org.infinispan.protostream.annotations.ProtoSyntax;

/**
 * For {@link IndexSchemaChangeTest}, represents the initial version of the entities.
 */
@ProtoSchema(
        syntax = ProtoSyntax.PROTO3,
        schemaPackageName = "keycloak.test",
        schemaFilePath = "proto/generated",

        includeClasses = {
                TestModelV1.AddIndexedFieldClass.class,
                TestModelV1.RemoveIndexedFieldClass.class,
                TestModelV1.ChangedIndexedFieldAttributeClass.class,
                TestModelV1.ChangedIndexedFieldClass.class,
                TestModelV1.NothingChangedIndexClass.class,
                TestModelV1.NothingChangedClass.class
        },

        service = false
)
public interface TestModelV1 extends GeneratedSchema {

    GeneratedSchema INSTANCE = new TestModelV1Impl();

    class AddIndexedFieldClass {

        @ProtoField(1)
        public String field1;

    }

    @Indexed
    class RemoveIndexedFieldClass {

        @ProtoField(1)
        public String field1;

        @ProtoField(2)
        @Basic
        public String field2;

    }

    @Indexed
    class ChangedIndexedFieldAttributeClass {

        @ProtoField(1)
        @Basic
        public String field1;

    }

    class ChangedIndexedFieldClass {

        @ProtoField(1)
        public String field1;

    }

    @Indexed
    class NothingChangedIndexClass {

        @ProtoField(1)
        @Basic
        public String field1;

    }

    class NothingChangedClass {

        @ProtoField(1)
        public String field1;

    }

}
