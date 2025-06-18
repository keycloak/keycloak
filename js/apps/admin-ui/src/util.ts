import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type { ProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepesentation";
import type { IFormatter, IFormatterValueType } from "@patternfly/react-table";
import { saveAs } from "file-saver";
import { flatten } from "flat";
import { cloneDeep } from "lodash-es";
import { FieldValues, Path, PathValue, UseFormSetValue } from "react-hook-form";
import {
  KeyValueType,
  arrayToKeyValue,
  keyValueToArray,
} from "./components/key-value-form/key-value-convert";
import { ReplaceString } from "./utils/types";

export const sortProviders = (providers: {
  [index: string]: ProviderRepresentation;
}) => {
  return [...new Map(Object.entries(providers).sort(sortProvider)).keys()];
};

const sortProvider = (
  a: [string, ProviderRepresentation],
  b: [string, ProviderRepresentation],
) => {
  let s1, s2;
  if (a[1].order !== b[1].order) {
    s1 = b[1].order;
    s2 = a[1].order;
  } else {
    s1 = a[0];
    s2 = b[0];
  }
  if (s1 < s2) {
    return -1;
  } else if (s1 > s2) {
    return 1;
  } else {
    return 0;
  }
};

export const toKey = (value: string) => value.replace(/\s/g, "-");

export const exportClient = (client: ClientRepresentation): void => {
  const clientCopy = cloneDeep(client);
  delete clientCopy.id;

  if (clientCopy.protocolMappers) {
    for (let i = 0; i < clientCopy.protocolMappers.length; i++) {
      delete clientCopy.protocolMappers[i].id;
    }
  }

  saveAs(
    new Blob([prettyPrintJSON(clientCopy)], {
      type: "application/json",
    }),
    clientCopy.clientId + ".json",
  );
};

export const toUpperCase = <T extends string>(name: T) =>
  (name.charAt(0).toUpperCase() + name.slice(1)) as Capitalize<T>;

const isAttributesObject = (value: any) =>
  Object.values(value).filter(
    (value) => Array.isArray(value) && value.length >= 1,
  ).length !== 0;

const isAttributeArray = (value: any) => {
  if (!Array.isArray(value)) {
    return false;
  }

  return value.some(
    (e) => Object.hasOwn(e, "key") && Object.hasOwn(e, "value"),
  );
};

const isEmpty = (obj: any) => Object.keys(obj).length === 0;

export function convertAttributeNameToForm<T>(
  name: string,
): PathValue<T, Path<T>> {
  const index = name.indexOf(".");
  return `${name.substring(0, index)}.${beerify(name.substring(index + 1))}` as PathValue<
    T,
    Path<T>
  >;
}

export const beerify = <T extends string>(name: T) =>
  name.replaceAll(".", "🍺") as ReplaceString<T, ".", "🍺">;

export const debeerify = <T extends string>(name: T) =>
  name.replaceAll("🍺", ".") as ReplaceString<T, "🍺", ".">;

export function convertToFormValues<T extends FieldValues>(
  obj: FieldValues,
  setValue: UseFormSetValue<T>,
) {
  Object.entries(obj).map((entry) => {
    const [key, value] = entry as [Path<T>, any];
    if (key === "attributes" && isAttributesObject(value)) {
      setValue(key, arrayToKeyValue(value as Record<string, string[]>));
    } else if (key === "config" || key === "attributes") {
      if (!isEmpty(value)) {
        const flattened: any = flatten(value, { safe: true });
        const convertedValues = Object.entries(flattened).map(([key, value]) =>
          Array.isArray(value) && value.length === 1
            ? [key, value[0]]
            : [key, value],
        );

        convertedValues.forEach(([k, v]) =>
          setValue(`${key}.${beerify(k)}` as Path<T>, v),
        );
      } else {
        setValue(key, undefined as PathValue<T, Path<T>>);
      }
    } else {
      setValue(key, value);
    }
  });
}

export function convertFormValuesToObject<T extends Record<string, any>, G = T>(
  obj: T,
): G {
  const result: any = {};
  Object.entries(obj).map(([key, value]) => {
    if (isAttributeArray(value)) {
      result[key] = keyValueToArray(value as KeyValueType[]);
    } else if (key === "config" || key === "attributes") {
      result[key] = Object.fromEntries(
        Object.entries(
          (value as Record<string, unknown> | undefined) || {},
        ).map(([k, v]) => [debeerify(k), v]),
      );
    } else {
      result[key] = value;
    }
  });
  return result;
}

export const emptyFormatter =
  (): IFormatter => (data?: IFormatterValueType) => {
    return data ? data : "—";
  };

export const upperCaseFormatter =
  (): IFormatter => (data?: IFormatterValueType) => {
    const value = data?.toString();

    return (value ? toUpperCase(value) : undefined) as string;
  };

export const capitalizeFirstLetterFormatter =
  (): IFormatter => (data?: IFormatterValueType) => {
    const value = data?.toString();

    return (
      value
        ? value.charAt(0).toUpperCase() + value.slice(1).toLowerCase()
        : undefined
    ) as string;
  };

export const alphaRegexPattern = /[^A-Za-z]/g;

export const emailRegexPattern =
  /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;

export const KEY_PROVIDER_TYPE = "org.keycloak.keys.KeyProvider";

export const prettyPrintJSON = (value: any) => JSON.stringify(value, null, 2);

export const addTrailingSlash = (url: string) =>
  url.endsWith("/") ? url : url + "/";

export const localeToDisplayName = (locale: string, displayLocale: string) => {
  try {
    return new Intl.DisplayNames([displayLocale], { type: "language" }).of(
      // This is mapping old locale codes to the new locale codes for Simplified and Traditional Chinese.
      // Once the existing locales have been moved, this code can be removed.
      locale === "zh-CN" ? "zh-HANS" : locale === "zh-TW" ? "zh-HANT" : locale,
    );
  } catch {
    return locale;
  }
};
