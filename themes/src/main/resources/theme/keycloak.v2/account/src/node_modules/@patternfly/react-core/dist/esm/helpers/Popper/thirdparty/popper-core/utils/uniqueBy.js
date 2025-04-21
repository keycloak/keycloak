// @ts-nocheck
/**
 * @param arr
 * @param fn
 */
export default function uniqueBy(arr, fn) {
    const identifiers = new Set();
    return arr.filter(item => {
        const identifier = fn(item);
        if (!identifiers.has(identifier)) {
            identifiers.add(identifier);
            return true;
        }
    });
}
//# sourceMappingURL=uniqueBy.js.map