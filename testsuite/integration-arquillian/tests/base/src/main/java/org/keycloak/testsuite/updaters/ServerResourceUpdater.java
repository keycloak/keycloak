/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.updaters;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 *
 * @author hmlnarik
 */
public abstract class ServerResourceUpdater<T extends ServerResourceUpdater, Res, Rep> implements Closeable {

    protected final Res resource;
    protected final Rep rep;
    protected final Rep origRep;
    protected Consumer<Rep> updater;
    protected boolean updated = false;

    public ServerResourceUpdater(Res resource, Supplier<Rep> representationGenerator, Consumer<Rep> updater) {
        this.resource = resource;
        this.updater = updater;

        // origRep and rep need to be two different instances
        this.origRep = representationGenerator.get();
        this.rep = representationGenerator.get();
    }

    public Res getResource() {
        return resource;
    }

    public T update() {
        performUpdate(origRep, rep);
        this.updated = true;
        return (T) this;
    }

    protected void performUpdate(Rep from, Rep to) {
        updater.accept(to);
    }

    public T updateWith(Consumer<Rep> representationUpdater) {
        representationUpdater.accept(this.rep);
        return (T) this;
    }

    @Override
    public void close() throws IOException {
        if (this.updated) {
            performUpdate(rep, origRep);
        }
    }

    /**
     * This function performs a set of single {@code add} and {@code remove} operations that represent the changes needed to
     * get collection {@code from} to the state of collection {@code to}. Since this is intended to work on collections of
     * names of objects but the {@code add} and {@code remove} functions operate on IDs of those objects, a conversion
     * is performed.
     *
     * @param <T> Type of the objects in the collections (e.g. names)
     * @param <V> Type of the objects required by add/remove functions (e.g. IDs)
     * @param from Initial collection
     * @param to Target collection
     * @param client2ServerConvertorGenerator Producer of the convertor. If not needed, just use {@code () -> Functions::identity}.
     *    This is intentionally a lazy-evaluated function variable because the conversion map often needs to be obtained from the
     *    server which can be slow operation. This function is called only if the two collections differ.
     * @param add Function to add
     * @param remove Function to remove
     */
    public static <T, V> void updateViaAddRemove(Collection<T> from, Collection<T> to, Supplier<Function<T, V>> client2ServerConvertorGenerator, Consumer<V> add, Consumer<V> remove) {
        if (Objects.equals(from, to)) {
            return;
        }

        Function<T, V> client2ServerConvertor = client2ServerConvertorGenerator.get();

        Set<V> current = from == null ? Collections.EMPTY_SET : from.stream().map(client2ServerConvertor).collect(Collectors.toSet());
        Set<V> expected = to == null ? Collections.EMPTY_SET : to.stream().map(client2ServerConvertor).collect(Collectors.toSet());

        expected.stream()
          .filter(role -> ! current.contains(role))
          .forEach(add);
        current.stream()
          .filter(role -> ! expected.contains(role))
          .forEach(remove);
    }

}
