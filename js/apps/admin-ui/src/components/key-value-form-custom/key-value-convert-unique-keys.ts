import { Path, PathValue } from "react-hook-form";

export type KeyValueType = { key: string; value: string };

export function keyValueToArray(attributeArray: KeyValueType[] = []) {
  const validAttributes = attributeArray.filter(({ key }) => key !== "");
  const result: Record<string, string> = {};

  for (const { key, value } of validAttributes) {
    result[key] = value;
  }

  return result;
}

export function arrayToKeyValue<T>(attributes: Record<string, string> = {}) {
  const result = Object.entries(attributes).map(([key, value]) => {
    return { key: key, value: value };
  });

  return result.concat({ key: "", value: "" }) as PathValue<T, Path<T>>;
}
