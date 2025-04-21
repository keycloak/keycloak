"use strict";
// @ts-nocheck
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @param str
 * @param args
 */
function format(str, ...args) {
    return [...args].reduce((p, c) => p.replace(/%s/, c), str);
}
exports.default = format;
//# sourceMappingURL=format.js.map