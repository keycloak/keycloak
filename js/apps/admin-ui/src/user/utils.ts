import { UserProfileAttribute } from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import { TFunction } from "i18next";

export const isBundleKey = (displayName?: string) =>
  displayName?.includes("${");
export const unWrap = (key: string) => key.substring(2, key.length - 1);

export const label = (attribute: UserProfileAttribute, t: TFunction) =>
  (isBundleKey(attribute.displayName)
    ? t(unWrap(attribute.displayName!))
    : attribute.displayName) || attribute.name;

const ROOT_ATTRIBUTES = ["username", "firstName", "lastName", "email"];

const isRootAttribute = (attr?: string) =>
  attr && ROOT_ATTRIBUTES.includes(attr);

export const fieldName = (attribute: UserProfileAttribute) =>
  `${isRootAttribute(attribute.name) ? "" : "attributes."}${attribute.name}`;
