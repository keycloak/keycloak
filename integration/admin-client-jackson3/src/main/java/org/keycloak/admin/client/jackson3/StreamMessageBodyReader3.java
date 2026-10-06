package org.keycloak.admin.client.jackson3;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;

import dev.resteasy.providers.jackson.ResteasyJacksonProvider;
import org.jboss.resteasy.plugins.providers.sse.EventInput;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.json.JsonMapper;

public class StreamMessageBodyReader3 implements MessageBodyReader<Stream<?>> {

    private final Object jacksonProvider;

    StreamMessageBodyReader3(Object jacksonProvider) {
        this.jacksonProvider = jacksonProvider;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Stream.class;
    }

    @Override
    public Stream<?> readFrom(Class<Stream<?>> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {

        JsonMapper mapper = resolveMapper(mediaType);
        JsonParser parser = mapper.createParser(entityStream);
        JsonToken token = parser.nextToken();
        if (token == null) {
            return null;
        }
        if (token != JsonToken.START_ARRAY) {
            entityStream.close();
            throw new IOException("Expected Array");
        }
        token = parser.nextToken();
        if (token == JsonToken.END_ARRAY) {
            return Stream.of();
        }
        if (token != JsonToken.START_OBJECT) {
            entityStream.close();
            throw new IOException("Expected Object");
        }

        Class<?> elementType = Object.class;
        if (genericType instanceof ParameterizedType) {
            Type[] typeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
            if (typeArguments.length > 0) {
                if (typeArguments[0] instanceof WildcardType) {
                    typeArguments = ((WildcardType) typeArguments[0]).getUpperBounds();
                }
                if (typeArguments.length > 0 && typeArguments[0] instanceof Class<?>) {
                    elementType = (Class<?>) typeArguments[0];
                }
            }
        }

        Iterator<?> iter = mapper.readValues(parser, elementType);
        Stream<?> targetStream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iter, Spliterator.ORDERED),
                false).onClose(() -> {
            try {
                entityStream.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        return (Stream<?>) Proxy.newProxyInstance(EventInput.class.getClassLoader(), new Class<?>[] {Stream.class, EventInput.class}, (proxy, method, args) -> {
            try {
                return method.invoke(targetStream, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }

    private JsonMapper resolveMapper(MediaType mediaType) {
        if (jacksonProvider instanceof ResteasyJacksonProvider) {
            return ((ResteasyJacksonProvider) jacksonProvider).locateMapper(Object.class, mediaType);
        }
        return Jackson3MapperHolder.MAPPER;
    }
}
