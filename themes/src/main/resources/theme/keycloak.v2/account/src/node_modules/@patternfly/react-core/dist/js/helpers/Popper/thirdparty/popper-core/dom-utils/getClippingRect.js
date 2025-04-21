"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
const enums_1 = require("../enums");
const getViewportRect_1 = tslib_1.__importDefault(require("./getViewportRect"));
const getDocumentRect_1 = tslib_1.__importDefault(require("./getDocumentRect"));
const listScrollParents_1 = tslib_1.__importDefault(require("./listScrollParents"));
const getOffsetParent_1 = tslib_1.__importDefault(require("./getOffsetParent"));
const getDocumentElement_1 = tslib_1.__importDefault(require("./getDocumentElement"));
const getComputedStyle_1 = tslib_1.__importDefault(require("./getComputedStyle"));
const instanceOf_1 = require("./instanceOf");
const getBoundingClientRect_1 = tslib_1.__importDefault(require("./getBoundingClientRect"));
const getParentNode_1 = tslib_1.__importDefault(require("./getParentNode"));
const contains_1 = tslib_1.__importDefault(require("./contains"));
const getNodeName_1 = tslib_1.__importDefault(require("./getNodeName"));
const rectToClientRect_1 = tslib_1.__importDefault(require("../utils/rectToClientRect"));
/**
 * @param element
 */
function getInnerBoundingClientRect(element) {
    const rect = getBoundingClientRect_1.default(element);
    rect.top = rect.top + element.clientTop;
    rect.left = rect.left + element.clientLeft;
    rect.bottom = rect.top + element.clientHeight;
    rect.right = rect.left + element.clientWidth;
    rect.width = element.clientWidth;
    rect.height = element.clientHeight;
    rect.x = rect.left;
    rect.y = rect.top;
    return rect;
}
/**
 * @param element
 * @param clippingParent
 */
function getClientRectFromMixedType(element, clippingParent) {
    return clippingParent === enums_1.viewport
        ? rectToClientRect_1.default(getViewportRect_1.default(element))
        : instanceOf_1.isHTMLElement(clippingParent)
            ? getInnerBoundingClientRect(clippingParent)
            : rectToClientRect_1.default(getDocumentRect_1.default(getDocumentElement_1.default(element)));
}
// A "clipping parent" is an overflowable container with the characteristic of
// clipping (or hiding) overflowing elements with a position different from
// `initial`
/**
 * @param element
 */
function getClippingParents(element) {
    const clippingParents = listScrollParents_1.default(getParentNode_1.default(element));
    const canEscapeClipping = ['absolute', 'fixed'].indexOf(getComputedStyle_1.default(element).position) >= 0;
    const clipperElement = canEscapeClipping && instanceOf_1.isHTMLElement(element) ? getOffsetParent_1.default(element) : element;
    if (!instanceOf_1.isElement(clipperElement)) {
        return [];
    }
    // $FlowFixMe: https://github.com/facebook/flow/issues/1414
    return clippingParents.filter(clippingParent => instanceOf_1.isElement(clippingParent) && contains_1.default(clippingParent, clipperElement) && getNodeName_1.default(clippingParent) !== 'body');
}
// Gets the maximum area that the element is visible in due to any number of
// clipping parents
/**
 * @param element
 * @param boundary
 * @param rootBoundary
 */
function getClippingRect(element, boundary, rootBoundary) {
    const mainClippingParents = boundary === 'clippingParents' ? getClippingParents(element) : [].concat(boundary);
    const clippingParents = [...mainClippingParents, rootBoundary];
    const firstClippingParent = clippingParents[0];
    const clippingRect = clippingParents.reduce((accRect, clippingParent) => {
        const rect = getClientRectFromMixedType(element, clippingParent);
        accRect.top = Math.max(rect.top, accRect.top);
        accRect.right = Math.min(rect.right, accRect.right);
        accRect.bottom = Math.min(rect.bottom, accRect.bottom);
        accRect.left = Math.max(rect.left, accRect.left);
        return accRect;
    }, getClientRectFromMixedType(element, firstClippingParent));
    clippingRect.width = clippingRect.right - clippingRect.left;
    clippingRect.height = clippingRect.bottom - clippingRect.top;
    clippingRect.x = clippingRect.left;
    clippingRect.y = clippingRect.top;
    return clippingRect;
}
exports.default = getClippingRect;
//# sourceMappingURL=getClippingRect.js.map