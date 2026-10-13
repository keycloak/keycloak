package org.keycloak.models.cache.infinispan;

import java.util.function.Supplier;

public class LazyModel<M> implements Supplier<M> {

    private final Supplier<M> supplier;
    private M model;

    public LazyModel(Supplier<M> supplier) {
        this.supplier = supplier;
    }

    @Override
    public M get() {
        if (model == null) {
            model = supplier.get();
        }
        return model;
    }
}
