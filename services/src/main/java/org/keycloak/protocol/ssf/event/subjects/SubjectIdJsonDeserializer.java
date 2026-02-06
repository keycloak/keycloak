package org.keycloak.protocol.ssf.event.subjects;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Custom dezerializer to deal with legacy SubjectIds.
 */
public class SubjectIdJsonDeserializer extends JsonDeserializer<SubjectId> {

    @Override
    public SubjectId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        // Extract the format field
        JsonNode formatNode = node.get("format");
        boolean legacyRiscEventType = false;
        if (formatNode == null) {
            // legacy subject type format for older OpenID RISC Event types, see: https://openid.net/specs/openid-risc-event-types-1_0.html
            formatNode = node.get("subject_type");
            if (formatNode != null && formatNode.isTextual()) {
                legacyRiscEventType = true;
            }
        }

        if (formatNode == null || !formatNode.isTextual()) {
            throw new IOException("Missing or invalid 'format' field in SubjectId");
        }

        String format = formatNode.asText();
        if (legacyRiscEventType) {
            // legacy subject type format for older OpenID RISC Event types, see: https://openid.net/specs/openid-risc-event-types-1_0.html
            format = format.replace("-","_");
        }
        Class<? extends SubjectId> subjectClass = SubjectIds.getSubjectIdType(format);

        if (subjectClass == null) {
            throw new SubjectParsingException("Unknown SubjectId format: " + format);
        }

        SubjectId subjectId = mapper.treeToValue(node, subjectClass);
        subjectId.setFormat(format);

        return subjectId;
    }
}
