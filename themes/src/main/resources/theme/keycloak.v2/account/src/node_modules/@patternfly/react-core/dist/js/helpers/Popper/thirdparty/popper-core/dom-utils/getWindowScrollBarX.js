"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
// @ts-nocheck
const getBoundingClientRect_1 = tslib_1.__importDefault(require("./getBoundingClientRect"));
const getDocumentElement_1 = tslib_1.__importDefault(require("./getDocumentElement"));
const getWindowScroll_1 = tslib_1.__importDefault(require("./getWindowScroll"));
/**
 * @param element
 */
function getWindowScrollBarX(element) {
    // If <html> has a CSS width greater than the viewport, then this will be
    // incorrect for RTL.
    // Popper 1 is broken in this case and never had a bug report so let's assume
    // it's not an issue. I don't think anyone ever specifies width on <html>
    // anyway.
    // Browsers where the left scrollbar doesn't cause an issue report `0` for
    // this (e.g. Edge 2019, IE11, Safari)
    return getBoundingClientRect_1.default(getDocumentElement_1.default(element)).left + getWindowScroll_1.default(element).scrollLeft;
}
exports.default = getWindowScrollBarX;
//# sourceMappingURL=getWindowScrollBarX.js.map