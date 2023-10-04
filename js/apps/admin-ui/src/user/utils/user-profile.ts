import { UserProfileAttributeMetadata } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";

export function isRequiredAttribute({
  required,
  validators,
}: UserProfileAttributeMetadata): boolean {
  // If the 'required' attribute is not defined, we can assume it is not required.
  if (typeof required === "undefined") {
    return false;
  }

  // If the 'required' is a boolean, we can use it directly.
  if (typeof required === "boolean") {
    // Check if required is true or if the validators include a validation that would make the attribute implicitly required.
    return required || hasRequiredValidators(validators);
  }

  // If the 'required' attribute is not a boolean, it will be an object with required roles and scopes.
  // TODO: I feel like this assumption (and the type) might be wrong, and this is actually always a boolean.
  return true;
}

/**
 * Checks whether the given validators include a validation that would make the attribute implicitly required.
 */
function hasRequiredValidators(validators?: Record<string, unknown>) {
  // If we don't have any validators, the attribute is not required.
  if (!validators) {
    return false;
  }

  // If the 'length' validator is defined and has a minimal length greater than zero the attribute is implicitly required.
  // We have to do a lot of defensive coding here, because we don't have type information for the validators.
  if (
    typeof validators.length === "object" &&
    validators.length !== null &&
    "min" in validators.length &&
    typeof validators.length.min === "number"
  ) {
    return validators.length.min > 0;
  }

  return false;
}
