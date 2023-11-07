import { UserProfileAttributeMetadata } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import { TFunction } from "i18next";

export const isBundleKey = (displayName?: string) =>
  displayName?.includes("${");
export const unWrap = (key: string) => key.substring(2, key.length - 1);

export const label = (
  text: string | undefined,
  fallback: string | undefined,
  t: TFunction,
) => (isBundleKey(text) ? t(unWrap(text!)) : text) || fallback;

export const labelAttribute = (
  attribute: UserProfileAttributeMetadata,
  t: TFunction,
) => label(attribute.displayName, attribute.name, t);

const ROOT_ATTRIBUTES = ["username", "firstName", "lastName", "email"];

export const isRootAttribute = (attr?: string) =>
  attr && ROOT_ATTRIBUTES.includes(attr);

export const fieldName = (attribute: UserProfileAttributeMetadata) =>
  isRootAttribute(attribute.name)
    ? attribute.name
    : `attributes.${attribute.name}`;

export const isLightweightUser = (userId?: string) =>
  userId?.startsWith("lightweight-");
