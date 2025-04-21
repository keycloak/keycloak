"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
// @ts-nocheck
const getComputedStyle_1 = tslib_1.__importDefault(require("./getComputedStyle"));
/**
 * @param element
 */
function isScrollParent(element) {
    // Firefox wants us to check `-x` and `-y` variations as well
    const { overflow, overflowX, overflowY } = getComputedStyle_1.default(element);
    return /auto|scroll|overlay|hidden/.test(overflow + overflowY + overflowX);
}
exports.default = isScrollParent;
//# sourceMappingURL=isScrollParent.js.map