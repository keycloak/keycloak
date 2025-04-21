// @ts-nocheck
import getWindow from './getWindow';
import getNodeName from './getNodeName';
import getComputedStyle from './getComputedStyle';
import { isHTMLElement } from './instanceOf';
import isTableElement from './isTableElement';
import getParentNode from './getParentNode';
import getDocumentElement from './getDocumentElement';
/**
 * @param element
 */
function getTrueOffsetParent(element) {
    if (!isHTMLElement(element) || // https://github.com/popperjs/popper-core/issues/837
        getComputedStyle(element).position === 'fixed') {
        return null;
    }
    const offsetParent = element.offsetParent;
    if (offsetParent) {
        const html = getDocumentElement(offsetParent);
        if (getNodeName(offsetParent) === 'body' &&
            getComputedStyle(offsetParent).position === 'static' &&
            getComputedStyle(html).position !== 'static') {
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
    let currentNode = getParentNode(element);
    while (isHTMLElement(currentNode) && ['html', 'body'].indexOf(getNodeName(currentNode)) < 0) {
        const css = getComputedStyle(currentNode);
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
export default function getOffsetParent(element) {
    const window = getWindow(element);
    let offsetParent = getTrueOffsetParent(element);
    while (offsetParent && isTableElement(offsetParent) && getComputedStyle(offsetParent).position === 'static') {
        offsetParent = getTrueOffsetParent(offsetParent);
    }
    if (offsetParent && getNodeName(offsetParent) === 'body' && getComputedStyle(offsetParent).position === 'static') {
        return window;
    }
    return offsetParent || getContainingBlock(element) || window;
}
//# sourceMappingURL=getOffsetParent.js.map