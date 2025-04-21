"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
const getDocumentElement_1 = tslib_1.__importDefault(require("./getDocumentElement"));
const getComputedStyle_1 = tslib_1.__importDefault(require("./getComputedStyle"));
const getWindowScrollBarX_1 = tslib_1.__importDefault(require("./getWindowScrollBarX"));
const getWindowScroll_1 = tslib_1.__importDefault(require("./getWindowScroll"));
// Gets the entire size of the scrollable document area, even extending outside
// of the `<html>` and `<body>` rect bounds if horizontally scrollable
/**
 * @param element
 */
function getDocumentRect(element) {
    const html = getDocumentElement_1.default(element);
    const winScroll = getWindowScroll_1.default(element);
    const body = element.ownerDocument.body;
    const width = Math.max(html.scrollWidth, html.clientWidth, body ? body.scrollWidth : 0, body ? body.clientWidth : 0);
    const height = Math.max(html.scrollHeight, html.clientHeight, body ? body.scrollHeight : 0, body ? body.clientHeight : 0);
    let x = -winScroll.scrollLeft + getWindowScrollBarX_1.default(element);
    const y = -winScroll.scrollTop;
    if (getComputedStyle_1.default(body || html).direction === 'rtl') {
        x += Math.max(html.clientWidth, body ? body.clientWidth : 0) - width;
    }
    return { width, height, x, y };
}
exports.default = getDocumentRect;
//# sourceMappingURL=getDocumentRect.js.map