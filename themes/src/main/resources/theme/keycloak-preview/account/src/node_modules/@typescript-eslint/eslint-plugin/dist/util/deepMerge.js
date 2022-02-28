"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * Check if the variable contains an object stricly rejecting arrays
 * @param obj an object
 * @returns `true` if obj is an object
 */
function isObjectNotArray(obj) {
    return typeof obj === 'object' && !Array.isArray(obj);
}
exports.isObjectNotArray = isObjectNotArray;
/**
 * Pure function - doesn't mutate either parameter!
 * Merges two objects together deeply, overwriting the properties in first with the properties in second
 * @param first The first object
 * @param second The second object
 * @returns a new object
 */
function deepMerge(first = {}, second = {}) {
    // get the unique set of keys across both objects
    const keys = new Set(Object.keys(first).concat(Object.keys(second)));
    return Array.from(keys).reduce((acc, key) => {
        const firstHasKey = key in first;
        const secondHasKey = key in second;
        if (firstHasKey && secondHasKey) {
            if (isObjectNotArray(first[key]) && isObjectNotArray(second[key])) {
                // object type
                acc[key] = deepMerge(first[key], second[key]);
            }
            else {
                // value type
                acc[key] = second[key];
            }
        }
        else if (firstHasKey) {
            acc[key] = first[key];
        }
        else {
            acc[key] = second[key];
        }
        return acc;
    }, {});
}
exports.deepMerge = deepMerge;
//# sourceMappingURL=deepMerge.js.map