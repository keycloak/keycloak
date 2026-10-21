import { Dispatch, useCallback, useMemo } from "react";
import { useStorageItem } from "./useStorageItem";

/**
 * A hook that acts similarly to React's `useState()`, but persists the state using [Web Storage API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Storage_API).
 * Automatically updates the value when modified in the context of another document (such as an open tab) trough the [`storage`](https://developer.mozilla.org/en-US/docs/Web/API/Window/storage_event) event.
 *
 * The value is serialized as [JSON](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/JSON) and therefore the value provided must be serializable as such.
 * Because the value is always serialized it will never be referentially equal to originally provided value.
 *
 * @param storageArea The storage area to target, must implement the [`Storage`](https://developer.mozilla.org/en-US/docs/Web/API/Storage) interface (such as [`localStorage`](https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage) and [`sessionStorage`](https://developer.mozilla.org/en-US/docs/Web/API/Window/sessionStorage)).
 * @param keyName The key of the item to get from storage, same as passed to [`Storage.getItem()`](https://developer.mozilla.org/en-US/docs/Web/API/Storage/getItem)
 * @param defaultValue The default value to fall back to in case no stored value was retrieved (must be serializable as JSON).
 */
export function useStoredState<S>(
  storageArea: Storage,
  keyName: string,
  defaultValue: S,
): [S, Dispatch<S>] {
  const defaultValueSerialized = useMemo(
    () => JSON.stringify(defaultValue),
    [defaultValue],
  );

  const [storedValue, setStoredValue] = useStorageItem(
    storageArea,
    keyName,
    defaultValueSerialized,
  );

  const value = useMemo<S>(() => JSON.parse(storedValue), [storedValue]);
  const setValue = useCallback(
    (value: S) => setStoredValue(JSON.stringify(value)),
    [],
  );

  return [value, setValue];
}
