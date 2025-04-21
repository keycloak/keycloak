// @ts-nocheck

/**
 * @param arr
 * @param fn
 */
export default function uniqueBy<T>(arr: T[], fn: (arg0: T) => any): T[] {
  const identifiers = new Set();

  return arr.filter(item => {
    const identifier = fn(item);

    if (!identifiers.has(identifier)) {
      identifiers.add(identifier);
      return true;
    }
  });
}
