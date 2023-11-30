import { TFunction } from "i18next";
import { TFuncKey } from "../i18n";
import { UserProfileAttributeMetadata } from "../api/representations";

export const isBundleKey = (displayName?: string) =>
  displayName?.includes("${");
export const unWrap = (key: string) => key.substring(2, key.length - 1);

export const label = (attribute: UserProfileAttributeMetadata, t: TFunction) =>
  (isBundleKey(attribute.displayName)
    ? t(unWrap(attribute.displayName!) as TFuncKey)
    : attribute.displayName) || attribute.name;

const ROOT_ATTRIBUTES = ["username", "firstName", "lastName", "email"];

const isRootAttribute = (attr?: string) =>
  attr && ROOT_ATTRIBUTES.includes(attr);

export const fieldName = (attribute: UserProfileAttributeMetadata) =>
  `${isRootAttribute(attribute.name) ? "" : "attributes."}${attribute.name}`;
