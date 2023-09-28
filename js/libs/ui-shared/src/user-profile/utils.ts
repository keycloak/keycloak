import { UserProfileFieldsProps } from "./UserProfileGroup";

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

export const fieldName = (attribute: UserProfileFieldsProps) =>
  `${isRootAttribute(attribute.name) ? "" : "attributes."}${attribute.name}`;

export type TranslationFunction = (key: unknown) => string;
