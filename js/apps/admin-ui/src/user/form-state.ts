import UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import {
  KeyValueType,
  arrayToKeyValue,
  keyValueToArray,
} from "../components/key-value-form/key-value-convert";
import { beerify, debeerify } from "../util";

export type UserFormFields = Omit<
  UIUserRepresentation,
  "attributes" | "userProfileMetadata" | "unmanagedAttributes"
> & {
  attributes?: KeyValueType[] | Record<string, string | string[]>;
  unmanagedAttributes?: KeyValueType[] | Record<string, string | string[]>;
};

export interface UIUserRepresentation extends UserRepresentation {
  unmanagedAttributes?: Record<string, string[]>;
}

export function toUserFormFields(data: UIUserRepresentation): UserFormFields {
  const attributes: Record<string, string | string[]> = {};
  Object.entries(data.attributes || {}).forEach(
    ([k, v]) => (attributes[beerify(k)] = v),
  );

  const unmanagedAttributes = arrayToKeyValue(data.unmanagedAttributes);
  return { ...data, attributes, unmanagedAttributes };
}

export function toUserRepresentation(
  data: UserFormFields,
): UIUserRepresentation {
  const username = data.username?.trim();
  const attributes = Array.isArray(data.attributes)
    ? keyValueToArray(data.attributes)
    : Object.fromEntries(
        Object.entries(data.attributes || {}).map(([k, v]) => [
          debeerify(k),
          v,
        ]),
      );
  const unmanagedAttributes = Array.isArray(data.unmanagedAttributes)
    ? keyValueToArray(data.unmanagedAttributes)
    : data.unmanagedAttributes;

  for (const key in unmanagedAttributes) {
    if (attributes && Object.hasOwn(attributes, key)) {
      throw Error(
        `Attribute ${key} is a managed attribute and is already available from the user details.`,
      );
    }
  }

  return {
    ...data,
    username,
    attributes: { ...unmanagedAttributes, ...attributes },
    unmanagedAttributes: undefined,
  };
}

export function filterManagedAttributes(
  attributes: Record<string, string[]> = {},
  unmanagedAttributes: Record<string, string[]> = {},
) {
  return Object.fromEntries(
    Object.entries(attributes).filter(
      ([key]) => !Object.hasOwn(unmanagedAttributes, key),
    ),
  );
}
