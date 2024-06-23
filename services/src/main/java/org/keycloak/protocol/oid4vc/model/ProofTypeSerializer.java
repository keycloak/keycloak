package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class ProofTypeSerializer extends StdSerializer<ProofType> {
    protected ProofTypeSerializer() {
        super(ProofType.class);
    }

    @Override
    public void serialize(ProofType value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.getValue()); // Serialize as the value
    }
}
