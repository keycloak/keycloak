"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @param modifiers
 */
function mergeByName(modifiers) {
    const merged = modifiers.reduce((merged, current) => {
        const existing = merged[current.name];
        merged[current.name] = existing
            ? Object.assign(Object.assign(Object.assign({}, existing), current), { options: Object.assign(Object.assign({}, existing.options), current.options), data: Object.assign(Object.assign({}, existing.data), current.data) }) : current;
        return merged;
    }, {});
    // IE11 does not support Object.values
    return Object.keys(merged).map(key => merged[key]);
}
exports.default = mergeByName;
//# sourceMappingURL=mergeByName.js.map