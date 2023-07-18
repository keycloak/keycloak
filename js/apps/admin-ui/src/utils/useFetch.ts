import { DependencyList, useEffect } from "react";
import { useErrorHandler } from "react-error-boundary";

/**
 * Util function to only set the state when the component is still mounted.
 *
 * It takes 2 functions one you do your adminClient call in and the other to set your state
 *
 * @example
 * useFetch(
 *  () => adminClient.components.findOne({ id }),
 *  (component) => setupForm(component),
 *  []
 * );
 *
 * @param adminClientCall use this to do your adminClient call
 * @param callback when the data is fetched this is where you set your state
 */
export function useFetch<T>(
  adminClientCall: () => Promise<T>,
  callback: (param: T) => void,
  deps?: DependencyList,
) {
  const onError = useErrorHandler();

  useEffect(() => {
    const controller = new AbortController();
    const { signal } = controller;

    adminClientCall()
      .then((result) => {
        if (!signal.aborted) {
          callback(result);
        }
      })
      .catch((error) => {
        if (!signal.aborted) {
          onError(error);
        }
      });

    return () => controller.abort();
  }, deps);
}
