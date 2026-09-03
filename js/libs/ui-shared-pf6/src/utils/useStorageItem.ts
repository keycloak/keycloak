import { Dispatch, useCallback, useEffect, useState } from "react";

/**
 * A hook that allows you to get a specific item stored by the [Web Storage API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Storage_API).
 * Automatically updates the value when modified in the context of another document (such as an open tab) trough the [`storage`](https://developer.mozilla.org/en-US/docs/Web/API/Window/storage_event) event.
 *
 * @param storageArea The storage area to target, must implement the [`Storage`](https://developer.mozilla.org/en-US/docs/Web/API/Storage) interface (such as [`localStorage`](https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage) and [`sessionStorage`](https://developer.mozilla.org/en-US/docs/Web/API/Window/sessionStorage)).
 * @param keyName The key of the item to get from storage, same as passed to [`Storage.getItem()`](https://developer.mozilla.org/en-US/docs/Web/API/Storage/getItem)
 * @param The default value to fall back to in case no stored value was retrieved.
 */
export function useStorageItem(
  storageArea: Storage,
  keyName: string,
  defaultValue: string,
): [string, Dispatch<string>] {
  const [value, setInnerValue] = useState(
    () => storageArea.getItem(keyName) ?? defaultValue,
  );

  const setValue = useCallback((newValue: string) => {
    setInnerValue(newValue);
    storageArea.setItem(keyName, newValue);
  }, []);

  useEffect(() => {
    // If the key name or storage area has changed, we want to update the value.
    // React will only set state if it actually changed, so no need to worry about re-renders.
    setInnerValue(storageArea.getItem(keyName) ?? defaultValue);

    // Subscribe to storage events so we can update the value when it is changed within the context of another document.
    window.addEventListener("storage", handleStorage);

    function handleStorage(event: StorageEvent) {
      // If the affected storage area is different we can ignore this event.
      // For example, if we're using session storage we're not interested in changes from local storage.
      if (event.storageArea !== storageArea) {
        return;
      }

      // If the event key is null then it means all storage was cleared.
      // Therefore we're interested in keys that are, or that match the key name.
      if (event.key === null || event.key === keyName) {
        setInnerValue(event.newValue ?? defaultValue);
      }
    }

    return () => window.removeEventListener("storage", handleStorage);
  }, [storageArea, keyName]);

  return [value, setValue];
}
