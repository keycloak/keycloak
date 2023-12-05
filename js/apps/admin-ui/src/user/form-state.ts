import UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import {
  KeyValueType,
  arrayToKeyValue,
  keyValueToArray,
} from "../components/key-value-form/key-value-convert";
import { beerify, debeerify } from "../util";

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
  let attributes: Record<string, string | string[]> = {};
  if (userProfileEnabled) {
    Object.entries(data.attributes || {}).forEach(
      ([k, v]) => (attributes[beerify(k)] = v),
    );
  } else {
    attributes = arrayToKeyValue(data.attributes);
  }

  return { ...data, attributes };
}

export function toUserRepresentation(data: UserFormFields): UserRepresentation {
  const username = data.username?.trim();
  const attributes = Array.isArray(data.attributes)
    ? keyValueToArray(data.attributes)
    : Object.fromEntries(
        Object.entries(data.attributes || {}).map(([k, v]) => [
          debeerify(k),
          v,
        ]),
      );

  return { ...data, username, attributes };
}
