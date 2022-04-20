export type KeyValueType = { key: string; value: string };

export const keyValueToArray = (
  attributeArray: KeyValueType[] = []
): Record<string, string[]> =>
  Object.fromEntries(
    attributeArray
      .filter(({ key }) => key !== "")
      .map(({ key, value }) => [key, [value]])
  );

export const arrayToKeyValue = (
  attributes: Record<string, string[]> = {}
): KeyValueType[] => {
  const result = Object.entries(attributes).flatMap(([key, value]) =>
    value.map<KeyValueType>((value) => ({ key, value }))
  );

  return result.concat({ key: "", value: "" });
};
