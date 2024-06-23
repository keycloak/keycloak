package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;
import java.util.Map;

public class ProofTypeDeserializer extends StdDeserializer<ProofType> {

    private static final Map<String, ProofType> PROOF_TYPE_MAP = Map.of(
            "jwt", ProofType.JWT,
            "cwt", ProofType.CWT,
            "ldp_vp", ProofType.LD_PROOF
    );

    protected ProofTypeDeserializer() {
        super(ProofType.class);
    }

    @Override
    public ProofType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();

        ProofType proofType = PROOF_TYPE_MAP.get(value);
        if (proofType != null) {
            return proofType;
        }

        throw new InvalidFormatException(p, "Invalid ProofType value: " + value, value, ProofType.class);
    }
}
