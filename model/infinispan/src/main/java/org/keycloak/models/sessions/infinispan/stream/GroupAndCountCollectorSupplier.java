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

package org.keycloak.models.sessions.infinispan.stream;

import java.lang.invoke.SerializedLambda;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.keycloak.marshalling.Marshalling;

import org.infinispan.commons.util.concurrent.CompletableFutures;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * A {@link Supplier} that returns a {@link Collector} to group and count elements.
 * <p>
 * Infinispan can marshall lambdas, by using {@link SerializedLambda} but it is not as efficient and ProtoStream
 * marshaller.
 *
 * @param <T> The type of the elements.
 */
@ProtoTypeId(Marshalling.GROUP_AND_COUNT_COLLECTOR_SUPPLIER)
public class GroupAndCountCollectorSupplier<T> implements Supplier<Collector<T, ?, Map<T, Long>>> {

    private static final GroupAndCountCollectorSupplier<?> INSTANCE = new GroupAndCountCollectorSupplier<>();

    private GroupAndCountCollectorSupplier() {
    }

    @ProtoFactory
    @SuppressWarnings("unchecked")
    public static <T1> GroupAndCountCollectorSupplier<T1> getInstance() {
        return (GroupAndCountCollectorSupplier<T1>) INSTANCE;
    }

    @Override
    public Collector<T, ?, Map<T, Long>> get() {
        return Collectors.groupingBy(CompletableFutures.identity(), Collectors.counting());
    }
}
