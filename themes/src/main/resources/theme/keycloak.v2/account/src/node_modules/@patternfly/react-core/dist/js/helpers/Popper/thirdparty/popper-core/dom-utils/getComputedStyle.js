"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
// @ts-nocheck
const getWindow_1 = tslib_1.__importDefault(require("./getWindow"));
/**
 * @param element
 */
function getComputedStyle(element) {
    return getWindow_1.default(element).getComputedStyle(element);
}
exports.default = getComputedStyle;
//# sourceMappingURL=getComputedStyle.js.map