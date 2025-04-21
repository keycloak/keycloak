"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
const getBoundingClientRect_1 = tslib_1.__importDefault(require("./getBoundingClientRect"));
const getNodeScroll_1 = tslib_1.__importDefault(require("./getNodeScroll"));
const getNodeName_1 = tslib_1.__importDefault(require("./getNodeName"));
const instanceOf_1 = require("./instanceOf");
const getWindowScrollBarX_1 = tslib_1.__importDefault(require("./getWindowScrollBarX"));
const getDocumentElement_1 = tslib_1.__importDefault(require("./getDocumentElement"));
const isScrollParent_1 = tslib_1.__importDefault(require("./isScrollParent"));
// Returns the composite rect of an element relative to its offsetParent.
// Composite means it takes into account transforms as well as layout.
/**
 * @param elementOrVirtualElement
 * @param offsetParent
 * @param isFixed
 */
function getCompositeRect(elementOrVirtualElement, offsetParent, isFixed = false) {
    const documentElement = getDocumentElement_1.default(offsetParent);
    const rect = getBoundingClientRect_1.default(elementOrVirtualElement);
    const isOffsetParentAnElement = instanceOf_1.isHTMLElement(offsetParent);
    let scroll = { scrollLeft: 0, scrollTop: 0 };
    let offsets = { x: 0, y: 0 };
    if (isOffsetParentAnElement || (!isOffsetParentAnElement && !isFixed)) {
        if (getNodeName_1.default(offsetParent) !== 'body' || // https://github.com/popperjs/popper-core/issues/1078
            isScrollParent_1.default(documentElement)) {
            scroll = getNodeScroll_1.default(offsetParent);
        }
        if (instanceOf_1.isHTMLElement(offsetParent)) {
            offsets = getBoundingClientRect_1.default(offsetParent);
            offsets.x += offsetParent.clientLeft;
            offsets.y += offsetParent.clientTop;
        }
        else if (documentElement) {
            offsets.x = getWindowScrollBarX_1.default(documentElement);
        }
    }
    return {
        x: rect.left + scroll.scrollLeft - offsets.x,
        y: rect.top + scroll.scrollTop - offsets.y,
        width: rect.width,
        height: rect.height
    };
}
exports.default = getCompositeRect;
//# sourceMappingURL=getCompositeRect.js.map