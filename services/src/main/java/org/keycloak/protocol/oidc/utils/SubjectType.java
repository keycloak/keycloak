package org.keycloak.protocol.oidc.utils;

public enum SubjectType {
    PUBLIC,
    PAIRWISE;

    public static SubjectType parse(String subjectTypeStr) {
        if (subjectTypeStr == null) {
            return PUBLIC;
        }
        return Enum.valueOf(SubjectType.class, subjectTypeStr.toUpperCase());
    }
}
