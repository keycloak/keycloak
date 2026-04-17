package org.keycloak.ssf.transmitter.emit;

import org.keycloak.models.OrganizationModel;
import org.keycloak.models.UserModel;

/**
 * Tuple of subjects resolved from the emitter's {@code sub_id}. Either
 * field may be {@code null} when the corresponding facet is absent
 * or unresolvable; both being null is reported as
 * {@link EmitEventStatus#SUBJECT_NOT_FOUND}.
 */
record EmitSubjectResolution(UserModel user, OrganizationModel organization) {
}
