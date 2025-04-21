"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
// @ts-nocheck
const getWindow_1 = tslib_1.__importDefault(require("./getWindow"));
/**
 * @param node
 */
function getWindowScroll(node) {
    const win = getWindow_1.default(node);
    const scrollLeft = win.pageXOffset;
    const scrollTop = win.pageYOffset;
    return {
        scrollLeft,
        scrollTop
    };
}
exports.default = getWindowScroll;
//# sourceMappingURL=getWindowScroll.js.map