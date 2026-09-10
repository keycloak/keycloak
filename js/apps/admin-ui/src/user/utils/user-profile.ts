import { UserProfileAttributeMetadata } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import { isUserProfileError } from "@keycloak/keycloak-ui-shared";
import { TFunction } from "i18next";

export function isRequiredAttribute({
  required,
  validators,
}: UserProfileAttributeMetadata): boolean {
  // Check if required is true or if the validators include a validation that would make the attribute implicitly required.
  return required || hasRequiredValidators(validators);
}

/**
 * Extracts error messages from a UserProfileError and formats them as a single string.
 * Handles both single error and multiple errors in the responseData structure.
 *
 * @param error - The error object (should be a UserProfileError)
 * @param t - Translation function
 * @returns Formatted error message string with all errors joined by semicolons
 */
export function extractUserProfileErrorMessages(
  error: unknown,
  t: TFunction,
): string {
  if (!isUserProfileError(error)) {
    return "";
  }

  const responseData = error.responseData as
    | { errors?: { errorMessage?: string; params?: string[] }[] }
    | { errorMessage?: string; params?: string[] };

  const errors =
    "errors" in responseData && responseData.errors
      ? responseData.errors
      : [responseData as { errorMessage?: string; params?: string[] }];

  const errorMessages = errors
    .map((e) => {
      const params = e.params
        ? Object.fromEntries(e.params.map((v, i) => [i.toString(), v]))
        : {};
      return t(e.errorMessage || "", {
        ...params,
        defaultValue: e.errorMessage,
      });
    })
    .filter((msg) => msg && msg.trim() !== "");

  return errorMessages.length > 0 ? errorMessages.join("; ") : "";
}

/**
 * Checks whether the given validators include a validation that would make the attribute implicitly required.
 */
function hasRequiredValidators(
  validators?: UserProfileAttributeMetadata["validators"],
): boolean {
  // If we don't have any validators, the attribute is not required.
  if (!validators) {
    return false;
  }

  // If the 'length' validator is defined and has a minimal length greater than zero the attribute is implicitly required.
  // We have to do a lot of defensive coding here, because we don't have type information for the validators.
  if (
    "length" in validators &&
    "min" in validators.length &&
    typeof validators.length.min === "number"
  ) {
    return validators.length.min > 0;
  }

  return false;
}
