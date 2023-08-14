/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.keycloak.json.StringOrArrayDeserializer;
import org.keycloak.json.StringOrArraySerializer;

/**
  This fuzzer targets the serialize method of the StringOrArraySerializer
  class and the deserialize method of the StringOrArrayDeserializer class.
  The fuzzer class implements Serializable interface and specifies the target
  classes to be used for serializing and deserializing of its String array
  field. The specification is done by using the Jackson databind annotation
  classes.
  */
public class JsonSerializerDeserializationFuzzer implements Serializable {
  // Define the JsonProperty name and the serializer / deserializer used
  // for this variable field using Jackson annotation classes.
  @JsonProperty("text")
  @JsonSerialize(using = StringOrArraySerializer.class)
  @JsonDeserialize(using = StringOrArrayDeserializer.class)
  protected String[] text;

  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    try {
      // Initializing the String array input for the serialize
      // and deserialize process
      Integer count = data.consumeInt(1, 10);
      String text = data.consumeRemainingAsString();
      String[] input = new String[count];
      for (int i = 0; i < count; i++) {
        input[i] = text;
      }

      // Initializing an instance of the fuzzer class with
      // the prepared String array
      JsonSerializerDeserializationFuzzer target = new JsonSerializerDeserializationFuzzer();
      target.text = input;

      // Serialize the instace of the fuzzer class which
      // will use the target Serializer to serialize the
      // String array field as specified by the annotation
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(target);

      // Deserialize the instace of the fuzzer class which
      // will use the target Deserializer to deserialize the
      // String array field as specified by the annotation
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      ObjectInputStream ois = new ObjectInputStream(bais);
      ois.readObject();
    } catch (IOException | ClassNotFoundException e) {
      // Known exception
    }
  }
}
