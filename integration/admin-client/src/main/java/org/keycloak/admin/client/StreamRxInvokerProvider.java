package org.keycloak.admin.client;

import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import jakarta.ws.rs.client.RxInvokerProvider;
import jakarta.ws.rs.client.SyncInvoker;

public class StreamRxInvokerProvider implements RxInvokerProvider<StreamRxInvoker> {

    @Override
    public boolean isProviderFor(Class<?> clazz) {
        return clazz.equals(Stream.class);
    }

    @Override
    public StreamRxInvoker getRxInvoker(SyncInvoker syncInvoker, ExecutorService executorService) {
        return new StreamRxInvoker(syncInvoker, executorService);
    }

}
