import type { DependencyList } from "react";
import { useEffect, useState } from "react";

/**
 * Function that creates a Promise. Receives an [AbortSignal](https://developer.mozilla.org/en-US/docs/Web/API/AbortSignal)
 * which is aborted when the component unmounts, or the dependencies of the hook have changed.
 */
export type PromiseFactoryFn<T> = (signal: AbortSignal) => Promise<T>;

/**
 * Function which is called with the value of the Promise when it resolves.
 */
export type PromiseResolvedFn<T> = (value: T) => void;

/**
 * Takes a function that creates a Promise and returns its resolved result through a callback.
 *
 * ```ts
 * const [products, setProducts] = useState();
 *
 * function getProducts() {
 *  return fetch('/api/products').then((res) => res.json());
 * }
 *
 * usePromise(() => getProducts(), setProducts);
 * ```
 *
 * Also takes a list of dependencies, when the dependencies change the Promise is recreated.
 *
 * ```ts
 * usePromise(() => getProduct(id), setProduct, [id]);
 * ```
 *
 * Can abort a fetch request, an [AbortSignal](https://developer.mozilla.org/en-US/docs/Web/API/AbortSignal) is provided from the factory function to do so.
 * This signal will be aborted if the component unmounts, or if the dependencies of the hook have changed.
 *
 * ```ts
 * usePromise((signal) => fetch(`/api/products/${id}`, { signal }).then((res) => res.json()), setProduct, [id]);
 * ```
 *
 * @param factory Function that creates the Promise.
 * @param callback Function that gets called with the value of the Promise when it resolves.
 * @param deps If present, Promise will be recreated if the values in the list change.
 */
export function usePromise<T>(
  factory: PromiseFactoryFn<T>,
  callback: PromiseResolvedFn<T>,
  deps: DependencyList = [],
) {
  const [error, setError] = useState<unknown>();
  useEffect(() => {
    const controller = new AbortController();
    const { signal } = controller;

    async function handlePromise() {
      // Try to resolve the Promise, if it fails, check if it was aborted.
      try {
        callback(await factory(signal));
      } catch (error) {
        // Ignore errors caused by aborting the Promise.
        if (error instanceof Error && error.name === "AbortError") {
          return;
        }

        setError(error);
      }
    }

    void handlePromise();

    // Abort the Promise when the component unmounts, or the dependencies change.
    return () => controller.abort();
  }, deps);

  // Rethrow other errors.
  if (error) {
    throw error;
  }
}
