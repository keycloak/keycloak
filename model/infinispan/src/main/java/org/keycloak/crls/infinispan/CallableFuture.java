package org.keycloak.crls.infinispan;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class CallableFuture<V> implements Callable<V> {

    final Future<V> future;

    CallableFuture(Future<V> future) {
        this.future = future;
    }

    @Override
    public V call() throws Exception {
        return future.get();
    }
}
