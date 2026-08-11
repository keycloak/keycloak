package org.keycloak.ssf.subject;

import org.keycloak.ssf.SsfException;

/**
 * Signals that an admin-supplied {@code (subjectType, subjectValue)}
 * shorthand could not be resolved to a known user or organization.
 * Distinct from the generic {@link SsfException} so the admin emit
 * endpoint can surface a category-specific {@code subject_not_found}
 * code, and carries the offending {@code subjectType}/{@code subjectValue}
 * as structured fields so callers (logging, response shaping) don't
 * have to parse them back out of the message.
 */
public class SubjectNotFoundException extends SsfException {

    private final String subjectType;
    private final String subjectValue;

    public SubjectNotFoundException(String subjectType, String subjectValue) {
        super("Subject not found for type=" + subjectType + " value=" + subjectValue);
        this.subjectType = subjectType;
        this.subjectValue = subjectValue;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public String getSubjectValue() {
        return subjectValue;
    }
}
