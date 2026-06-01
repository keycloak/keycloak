package org.keycloak.representations.admin.v2.validators;

import org.keycloak.representations.admin.v2.RepresentationWithUuid;
import org.keycloak.validation.jakarta.ValidationContext;

/**
 * Used by {@link UuidUnmodifiedValidator} to get the persisted alias for a given UUID and to get the alias
 * from the type specific representation.
 *
 * @author Vaclav Muzikar <vmuzikar@ibm.com>
 */
public interface UuidProvider {
    boolean uuidExists(ValidationContext context, String uuid);
    String getPersistedUuid(ValidationContext context, RepresentationWithUuid representation);
}
