package org.keycloak.protocol.ssf.event.subjects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubjectIds {

    public final static Map<String, Class<? extends SubjectId>> SUBJECT_ID_FORMAT_TYPES;

    static {
        var map = new HashMap<String, Class<? extends SubjectId>>();
        List.of(//
                new AccountSubjectId(), //
                new AliasesSubjectId(), //
                new ComplexSubjectId(), //
                new DidSubjectId(), //
                new EmailSubjectId(), //
                new IssuerSubjectId(), //
                new JwtSubjectId(), //
                new OpaqueSubjectId(), //
                new PhoneNumberSubjectId(), //
                new SamlAssertionSubjectId(), //
                new UriSubjectId() //
        ).forEach(subjectId -> map.put(subjectId.getFormat(), subjectId.getClass()));
        SUBJECT_ID_FORMAT_TYPES = map;
    }

    public static Class<? extends SubjectId> getSubjectIdType(String format) {
        var subjectIdType = SUBJECT_ID_FORMAT_TYPES.get(format);
        if (subjectIdType != null) {
            return subjectIdType;
        }
        return GenericSubjectId.class;
    }
}
