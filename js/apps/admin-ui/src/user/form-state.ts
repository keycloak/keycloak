import UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import {
  KeyValueType,
  arrayToKeyValue,
  keyValueToArray,
} from "../components/key-value-form/key-value-convert";

export type UserFormFields = Omit<
  UserRepresentation,
  "attributes" | "userProfileMetadata"
> & {
  attributes?: KeyValueType[] | Record<string, string | string[]>;
};

export function toUserFormFields(
  data: UserRepresentation,
  userProfileEnabled: boolean,
): UserFormFields {
  const attributes = userProfileEnabled
    ? data.attributes
    : arrayToKeyValue(data.attributes);

  return { ...data, attributes };
}

export function toUserRepresentation(data: UserFormFields): UserRepresentation {
  const username = data.username?.trim();
  const attributes = Array.isArray(data.attributes)
    ? keyValueToArray(data.attributes)
    : data.attributes;

  return { ...data, username, attributes };
}
