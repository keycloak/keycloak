package org.keycloak.admin.client.deserializer;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

public class StreamDeserializer extends JsonDeserializer<Stream<?>> implements ContextualDeserializer {

  private final JavaType contentType;

  public StreamDeserializer() {
    this(null);
  }

  public StreamDeserializer(JavaType contentType) {
    this.contentType = contentType;
  }

  @Override
  public Stream<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JavaType streamType = ctxt.getContextualType();
    JavaType actualContentType = contentType;

    if (actualContentType == null && streamType != null && streamType.containedTypeCount() == 1) {
      actualContentType = streamType.containedType(0);
    }

    if (actualContentType == null) {
      actualContentType = ctxt.getTypeFactory().constructType(Object.class);
    }

    JavaType listType = ctxt.getTypeFactory().constructCollectionType(List.class, actualContentType);
    List<?> list = ctxt.readValue(p, listType);
    return list.stream();
  }

  @Override
  public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
    JavaType contextualType = ctxt.getContextualType();
    JavaType actualContentType = null;

    if (contextualType != null && contextualType.containedTypeCount() == 1) {
      actualContentType = contextualType.containedType(0);
    }

    return new StreamDeserializer(actualContentType);
  }
}
