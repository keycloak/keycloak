// @ts-nocheck

/**
 * @param value
 * @param keys
 */
export default function expandToHashMap<T extends number | string | boolean, K extends string>(
  value: T,
  keys: K[]
): {
  [key: string]: T;
} {
  return keys.reduce((hashMap, key) => {
    hashMap[key] = value;
    return hashMap;
  }, {});
}
