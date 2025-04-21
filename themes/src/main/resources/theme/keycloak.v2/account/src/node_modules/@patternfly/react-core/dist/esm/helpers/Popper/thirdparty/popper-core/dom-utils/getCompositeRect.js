import getBoundingClientRect from './getBoundingClientRect';
import getNodeScroll from './getNodeScroll';
import getNodeName from './getNodeName';
import { isHTMLElement } from './instanceOf';
import getWindowScrollBarX from './getWindowScrollBarX';
import getDocumentElement from './getDocumentElement';
import isScrollParent from './isScrollParent';
// Returns the composite rect of an element relative to its offsetParent.
// Composite means it takes into account transforms as well as layout.
/**
 * @param elementOrVirtualElement
 * @param offsetParent
 * @param isFixed
 */
export default function getCompositeRect(elementOrVirtualElement, offsetParent, isFixed = false) {
    const documentElement = getDocumentElement(offsetParent);
    const rect = getBoundingClientRect(elementOrVirtualElement);
    const isOffsetParentAnElement = isHTMLElement(offsetParent);
    let scroll = { scrollLeft: 0, scrollTop: 0 };
    let offsets = { x: 0, y: 0 };
    if (isOffsetParentAnElement || (!isOffsetParentAnElement && !isFixed)) {
        if (getNodeName(offsetParent) !== 'body' || // https://github.com/popperjs/popper-core/issues/1078
            isScrollParent(documentElement)) {
            scroll = getNodeScroll(offsetParent);
        }
        if (isHTMLElement(offsetParent)) {
            offsets = getBoundingClientRect(offsetParent);
            offsets.x += offsetParent.clientLeft;
            offsets.y += offsetParent.clientTop;
        }
        else if (documentElement) {
            offsets.x = getWindowScrollBarX(documentElement);
        }
    }
    return {
        x: rect.left + scroll.scrollLeft - offsets.x,
        y: rect.top + scroll.scrollTop - offsets.y,
        width: rect.width,
        height: rect.height
    };
}
//# sourceMappingURL=getCompositeRect.js.map