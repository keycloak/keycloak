import { UserProfileFieldsProps } from "./UserProfileGroup";
import { FieldError, UserProfileError } from "./userProfileConfig";

export const isBundleKey = (displayName?: string) =>
  displayName?.includes("${");
export const unWrap = (key: string) => key.substring(2, key.length - 1);

export const label = ({ t, ...attribute }: UserProfileFieldsProps) =>
  (isBundleKey(attribute.displayName)
    ? t(unWrap(attribute.displayName!) as string)
    : attribute.displayName) || attribute.name;

const ROOT_ATTRIBUTES = ["username", "firstName", "lastName", "email"];

const isRootAttribute = (attr?: string) =>
  attr && ROOT_ATTRIBUTES.includes(attr);

export const fieldName = (name?: string) =>
  `${isRootAttribute(name) ? "" : "attributes."}${name}`;

export function setUserProfileServerError<T>(
  error: unknown,
  setError: (field: keyof T, params: object) => void,
  t: TranslationFunction,
) {
  (error as FieldError[]).forEach((e) => {
    const params = Object.assign(
      {},
      e.params?.map((p) => t(isBundleKey(p) ? unWrap(p) : p)),
    );
    console.log("why", params, e.errorMessage);
    setError(fieldName(e.field) as keyof T, {
      message: t(e.errorMessage, {
        ...params,
        defaultValue: e.field,
      }),
      type: "server",
    });
  });
}

export function isUserProfileError(error: unknown): error is UserProfileError {
  return !!(error as UserProfileError).responseData?.errors;
}

export type TranslationFunction = (key: unknown, params?: object) => string;
