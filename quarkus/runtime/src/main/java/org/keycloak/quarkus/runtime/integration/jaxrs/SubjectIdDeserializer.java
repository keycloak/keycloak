package org.keycloak.quarkus.runtime.integration.jaxrs;

import org.keycloak.ssf.subject.SubjectId;
import org.keycloak.ssf.subject.SubjectIds;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

final class SubjectIdDeserializer extends ValueDeserializer<SubjectId> {

    @Override
    public SubjectId deserialize(JsonParser parser, DeserializationContext context) {
        var node = context.readTree(parser);
        var formatNode = node.get("format");
        boolean legacyRiscEventType = false;
        if (formatNode == null) {
            formatNode = node.get("subject_type");
            legacyRiscEventType = formatNode != null && formatNode.isString();
        }
        if (formatNode == null || !formatNode.isString()) {
            return context.reportInputMismatch(SubjectId.class, "Missing or invalid 'format' field in SubjectId");
        }

        String format = formatNode.asString();
        if (legacyRiscEventType) {
            format = format.replace('-', '_');
        }
        var subject = context.readTreeAsValue(node, SubjectIds.getSubjectIdType(format));
        subject.setFormat(format);
        return subject;
    }
}
