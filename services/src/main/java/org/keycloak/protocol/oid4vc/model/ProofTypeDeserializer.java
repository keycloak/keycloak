/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;
import java.util.Map;

/**
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
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
