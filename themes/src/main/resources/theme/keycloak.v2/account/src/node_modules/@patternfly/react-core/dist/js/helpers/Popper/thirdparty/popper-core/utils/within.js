"use strict";
// @ts-nocheck
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @param min
 * @param value
 * @param max
 */
function within(min, value, max) {
    return Math.max(min, Math.min(value, max));
}
exports.default = within;
//# sourceMappingURL=within.js.map