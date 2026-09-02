package org.keycloak.ssf.subject;

import org.keycloak.models.OrganizationModel;
import org.keycloak.models.UserModel;

/**
 * Result of a {@link SubjectResolver#resolve} call. Unsealed so
 * third-party code can add custom variants (e.g. a
 * {@code Group(GroupModel)} record) and handle them via overridden
 * {@code resolveSubject} / {@code applySubjectResolution} methods
 * in the transmitter's extension points.
 */
public interface SubjectResolution {

    SubjectResolution NOT_FOUND = new NotFound();

    SubjectResolution UNSUPPORTED_FORMAT = new UnsupportedFormat();

    record User(UserModel user) implements SubjectResolution {}

    record Organization(OrganizationModel organization) implements SubjectResolution {}

    record NotFound() implements SubjectResolution {}

    record UnsupportedFormat() implements SubjectResolution {}
}
