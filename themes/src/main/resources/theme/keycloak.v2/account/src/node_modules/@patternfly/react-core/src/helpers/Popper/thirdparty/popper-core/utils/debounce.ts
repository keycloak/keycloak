// @ts-nocheck

/**
 * @param fn
 */
export default function debounce<T>(fn: Function): () => Promise<T> {
  let pending;
  return () => {
    if (!pending) {
      pending = new Promise<T>(resolve => {
        Promise.resolve().then(() => {
          pending = undefined;
          resolve(fn());
        });
      });
    }

    return pending;
  };
}
