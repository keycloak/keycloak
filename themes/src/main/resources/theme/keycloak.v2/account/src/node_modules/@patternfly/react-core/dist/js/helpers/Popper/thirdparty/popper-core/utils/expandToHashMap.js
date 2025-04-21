"use strict";
// @ts-nocheck
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @param value
 * @param keys
 */
function expandToHashMap(value, keys) {
    return keys.reduce((hashMap, key) => {
        hashMap[key] = value;
        return hashMap;
    }, {});
}
exports.default = expandToHashMap;
//# sourceMappingURL=expandToHashMap.js.map