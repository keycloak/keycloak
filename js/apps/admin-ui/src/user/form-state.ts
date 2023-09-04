import UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import {
  KeyValueType,
  arrayToKeyValue,
  keyValueToArray,
} from "../components/key-value-form/key-value-convert";

export type UserFormFields = Omit<UserRepresentation, "userProfileMetadata"> & {
  attributes?: KeyValueType[];
};

export function toUserFormFields(data: UserRepresentation): UserFormFields {
  const attributes = arrayToKeyValue(data.attributes);

  return { ...data, attributes };
}

export function toUserRepresentation(data: UserFormFields): UserRepresentation {
  const username = data.username?.trim();
  const attributes = keyValueToArray(data.attributes);

  return { ...data, username, attributes };
}
