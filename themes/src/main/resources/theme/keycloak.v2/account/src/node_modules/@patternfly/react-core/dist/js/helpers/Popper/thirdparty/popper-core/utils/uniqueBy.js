"use strict";
// @ts-nocheck
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @param arr
 * @param fn
 */
function uniqueBy(arr, fn) {
    const identifiers = new Set();
    return arr.filter(item => {
        const identifier = fn(item);
        if (!identifiers.has(identifier)) {
            identifiers.add(identifier);
            return true;
        }
    });
}
exports.default = uniqueBy;
//# sourceMappingURL=uniqueBy.js.map