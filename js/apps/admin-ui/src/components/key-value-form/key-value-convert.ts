import { Path, PathValue } from "react-hook-form";

export type KeyValueType = { key: string; value: string };

export function keyValueToArray(attributeArray: KeyValueType[] = []) {
  const validAttributes = attributeArray.filter(({ key }) => key !== "");
  const result: Record<string, string[]> = {};

  for (const { key, value } of validAttributes) {
    if (key in result) {
      result[key].push(value);
    } else {
      result[key] = [value];
    }
  }

  return result;
}

export function arrayToKeyValue<T>(attributes: Record<string, string[]> = {}) {
  const result = Object.entries(attributes).flatMap(([key, value]) =>
    value.map<KeyValueType>((value) => ({ key, value })),
  );

  return result as PathValue<T, Path<T>>;
}
