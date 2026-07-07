package org.keycloak.representations.admin.v2.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.keycloak.representations.admin.v2.validation.ServerManagedFieldUnmodified;

/**
 * Validates that server-managed fields provided by the client are not specified, or equal to the persisted values
 * (in case of an update).
 * <p>
 * When {@link ServerManagedFieldUnmodified#rejectExistingValueOnCreate()} is enabled, it additionally checks that
 * the provided value does not already exist in the system to prevent re-creation of a renamed resource.
 * This is useful for PUT create.
 * <p>
 * It assumes that the resource has a unique alias (e.g. name or clientId) that is used to identify the resource
 * in addition to the server-managed field.
 *
 * @author Vaclav Muzikar <vmuzikar@ibm.com>
 */
public class ServerManagedFieldUnmodifiedValidator implements ConstraintValidator<ServerManagedFieldUnmodified, Object> {

    private String[] affectedFieldNames;

    @Override
    public void initialize(ServerManagedFieldUnmodified annotation) {
        this.affectedFieldNames = annotation.affectedFieldNames();
    }

    @Override
    public boolean isValid(Object representation, ConstraintValidatorContext context) {
        return ServerManagedFieldValidation.isValid(representation, affectedFieldNames, context);
    }
}
