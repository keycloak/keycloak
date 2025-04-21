"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
// @ts-nocheck
const getWindow_1 = tslib_1.__importDefault(require("./getWindow"));
const getNodeName_1 = tslib_1.__importDefault(require("./getNodeName"));
const getComputedStyle_1 = tslib_1.__importDefault(require("./getComputedStyle"));
const instanceOf_1 = require("./instanceOf");
const isTableElement_1 = tslib_1.__importDefault(require("./isTableElement"));
const getParentNode_1 = tslib_1.__importDefault(require("./getParentNode"));
const getDocumentElement_1 = tslib_1.__importDefault(require("./getDocumentElement"));
/**
 * @param element
 */
function getTrueOffsetParent(element) {
    if (!instanceOf_1.isHTMLElement(element) || // https://github.com/popperjs/popper-core/issues/837
        getComputedStyle_1.default(element).position === 'fixed') {
        return null;
    }
    const offsetParent = element.offsetParent;
    if (offsetParent) {
        const html = getDocumentElement_1.default(offsetParent);
        if (getNodeName_1.default(offsetParent) === 'body' &&
            getComputedStyle_1.default(offsetParent).position === 'static' &&
            getComputedStyle_1.default(html).position !== 'static') {
            return html;
        }
    }
    return offsetParent;
}
// `.offsetParent` reports `null` for fixed elements, while absolute elements
// return the containing block
/**
 * @param element
 */
function getContainingBlock(element) {
    let currentNode = getParentNode_1.default(element);
    while (instanceOf_1.isHTMLElement(currentNode) && ['html', 'body'].indexOf(getNodeName_1.default(currentNode)) < 0) {
        const css = getComputedStyle_1.default(currentNode);
        // This is non-exhaustive but covers the most common CSS properties that
        // create a containing block.
        if (css.transform !== 'none' || css.perspective !== 'none' || (css.willChange && css.willChange !== 'auto')) {
            return currentNode;
        }
        else {
            currentNode = currentNode.parentNode;
        }
    }
    return null;
}
// Gets the closest ancestor positioned element. Handles some edge cases,
// such as table ancestors and cross browser bugs.
/**
 * @param element
 */
function getOffsetParent(element) {
    const window = getWindow_1.default(element);
    let offsetParent = getTrueOffsetParent(element);
    while (offsetParent && isTableElement_1.default(offsetParent) && getComputedStyle_1.default(offsetParent).position === 'static') {
        offsetParent = getTrueOffsetParent(offsetParent);
    }
    if (offsetParent && getNodeName_1.default(offsetParent) === 'body' && getComputedStyle_1.default(offsetParent).position === 'static') {
        return window;
    }
    return offsetParent || getContainingBlock(element) || window;
}
exports.default = getOffsetParent;
//# sourceMappingURL=getOffsetParent.js.map