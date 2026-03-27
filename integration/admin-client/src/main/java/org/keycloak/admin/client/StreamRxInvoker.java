package org.keycloak.admin.client;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.json.JsonMapper;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.ResponseProcessingException;
import jakarta.ws.rs.client.RxInvoker;
import jakarta.ws.rs.client.SyncInvoker;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

public class StreamRxInvoker implements RxInvoker<Stream<?>> {

    private SyncInvoker syncInvoker;
    private ExecutorService executorService;

    public StreamRxInvoker(SyncInvoker syncInvoker, ExecutorService executorService) {
        this.syncInvoker = syncInvoker;
        this.executorService = executorService;
    }

    @Override
    public Stream<?> get() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> Stream<?> get(Class<R> responseType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> Stream<?> get(GenericType<R> responseType) {
        Response response = this.syncInvoker.get(Response.class);

        if (response.getStatus() != 200) {
            response.close();
            throw new AssertionError(); // TODO
        }

        InputStream stream = (InputStream)response.getEntity();

        JsonMapper mapper = new JsonMapper(); // TODO: obtain a better instance
        try {
            MappingIterator<?> iter = mapper.readerFor(responseType.getRawType()).readValues(stream);

            Stream<?> targetStream = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(iter, Spliterator.ORDERED),
                    false);
            return (Stream<?>) Proxy.newProxyInstance(Stream.class.getClassLoader(), new Class<?>[] {Stream.class}, (proxy, method, args) -> {
                if (method.getName().equals("close")) {
                    response.close();
                }
                return method.invoke(targetStream, args);
            });
        } catch (IOException e) {
            response.close();
            throw new ResponseProcessingException(response, "Error", e);
        }
    }

    @Override
    public Stream<?> put(Entity<?> entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> Stream<?> put(Entity<?> entity, Class<R> responseType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> Stream<?> put(Entity<?> entity, GenericType<R> responseType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<?> post(Entity<?> entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> Stream<?> post(Entity<?> entity, Class<R> responseType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> Stream<?> post(Entity<?> entity, GenericType<R> responseType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<?> delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> Stream<?> delete(Class<R> responseType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> Stream<?> delete(GenericType<R> responseType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<?> head() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<?> options() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> Stream<?> options(Class<R> responseType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> Stream<?> options(GenericType<R> responseType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<?> trace() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> Stream<?> trace(Class<R> responseType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> Stream<?> trace(GenericType<R> responseType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<?> method(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> Stream<?> method(String name, Class<R> responseType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> Stream<?> method(String name, GenericType<R> responseType) {
        if(!name.equals("GET")) {
            throw new UnsupportedOperationException();
        }
        return get(responseType);
    }

    @Override
    public Stream<?> method(String name, Entity<?> entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> Stream<?> method(String name, Entity<?> entity, Class<R> responseType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> Stream<?> method(String name, Entity<?> entity, GenericType<R> responseType) {
        throw new UnsupportedOperationException();
    }

}
