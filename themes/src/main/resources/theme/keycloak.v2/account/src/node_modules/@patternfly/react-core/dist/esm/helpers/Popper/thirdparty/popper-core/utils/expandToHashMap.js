// @ts-nocheck
/**
 * @param value
 * @param keys
 */
export default function expandToHashMap(value, keys) {
    return keys.reduce((hashMap, key) => {
        hashMap[key] = value;
        return hashMap;
    }, {});
}
//# sourceMappingURL=expandToHashMap.js.map