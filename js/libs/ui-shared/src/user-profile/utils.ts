import { UserProfileAttributeMetadata } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { TFunction } from "i18next";
import { FieldPath } from "react-hook-form";

export type KeyValueType = { key: string; value: string };

export type UserFormFields = Omit<
  UserRepresentation,
  "attributes" | "userProfileMetadata"
> & {
  attributes?: KeyValueType[] | Record<string, string | string[]>;
};

type FieldError = {
  field: string;
  errorMessage: string;
  params?: unknown[];
};

type ErrorArray = { errors?: FieldError[] };

export type UserProfileError = {
  responseData: ErrorArray | FieldError;
};

const isBundleKey = (displayName: unknown) => {
  return displayName && typeof displayName === "string"
    ? displayName.includes("${")
    : false;
};
const unWrap = (key: string) => key.substring(2, key.length - 1);

export const label = (
  t: TFunction,
  text: string | undefined,
  fallback?: string,
  prefix?: string,
) => {
  const value = text || fallback;
  const bundleKey = isBundleKey(value) ? unWrap(value!) : value;
  const key = prefix ? `${prefix}.${bundleKey}` : bundleKey;
  return t(key || "");
};

export const labelAttribute = (
  t: TFunction,
  attribute: UserProfileAttributeMetadata,
) => label(t, attribute.displayName, attribute.name);

const ROOT_ATTRIBUTES = ["username", "firstName", "lastName", "email"];

export const isRootAttribute = (attr?: string) =>
  attr && ROOT_ATTRIBUTES.includes(attr);

export const fieldName = (name?: string) =>
  `${isRootAttribute(name) ? "" : "attributes."}${name?.replaceAll(
    ".",
    "🍺",
  )}` as FieldPath<UserFormFields>;

export const beerify = <T extends string>(name: T) =>
  name.replaceAll(".", "🍺");

export const debeerify = <T extends string>(name: T) =>
  name.replaceAll("🍺", ".");

export function setUserProfileServerError<T>(
  error: UserProfileError,
  setError: (field: keyof T, params: object) => void,
  t: TFunction,
) {
  (
    ((error.responseData as ErrorArray).errors !== undefined
      ? (error.responseData as ErrorArray).errors
      : [error.responseData]) as FieldError[]
  ).forEach((e) => {
    const params = Object.assign(
      {},
      e.params?.map((p) => (isBundleKey(p) ? t(unWrap(p as string)) : p)),
    );
    setError(fieldName(e.field) as keyof T, {
      message: t(
        isBundleKey(e.errorMessage) ? unWrap(e.errorMessage) : e.errorMessage,
        {
          /* eslint-disable @typescript-eslint/no-misused-spread */
          ...params,
          defaultValue: e.errorMessage || e.field,
        },
      ),
      type: "server",
    });
  });
}

export function isRequiredAttribute({
  required,
}: UserProfileAttributeMetadata): boolean {
  // Check if required is true
  return required as boolean;
}

export function isUserProfileError(error: unknown): error is UserProfileError {
  // Check if the error is an object with a 'responseData' property.
  if (
    typeof error !== "object" ||
    error === null ||
    !("responseData" in error)
  ) {
    return false;
  }

  const { responseData } = error;

  if (isFieldError(responseData)) {
    return true;
  }

  // Check if 'responseData' is an object with an 'errors' property that is an array.
  if (
    typeof responseData !== "object" ||
    responseData === null ||
    !("errors" in responseData) ||
    !Array.isArray(responseData.errors)
  ) {
    return false;
  }

  // Check if all errors are field errors.
  return responseData.errors.every(isFieldError);
}

function isFieldError(error: unknown): error is FieldError {
  // Check if the error is an object.
  if (typeof error !== "object" || error === null) {
    return false;
  }

  // Check if the error object has a 'field' property that is a string.
  if (!("field" in error) || typeof error.field !== "string") {
    return false;
  }

  // Check if the error object has an 'errorMessage' property that is a string.
  if (!("errorMessage" in error) || typeof error.errorMessage !== "string") {
    return false;
  }

  return true;
}
