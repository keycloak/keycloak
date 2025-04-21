import { viewport } from '../enums';
import getViewportRect from './getViewportRect';
import getDocumentRect from './getDocumentRect';
import listScrollParents from './listScrollParents';
import getOffsetParent from './getOffsetParent';
import getDocumentElement from './getDocumentElement';
import getComputedStyle from './getComputedStyle';
import { isElement, isHTMLElement } from './instanceOf';
import getBoundingClientRect from './getBoundingClientRect';
import getParentNode from './getParentNode';
import contains from './contains';
import getNodeName from './getNodeName';
import rectToClientRect from '../utils/rectToClientRect';
/**
 * @param element
 */
function getInnerBoundingClientRect(element) {
    const rect = getBoundingClientRect(element);
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
    return clippingParent === viewport
        ? rectToClientRect(getViewportRect(element))
        : isHTMLElement(clippingParent)
            ? getInnerBoundingClientRect(clippingParent)
            : rectToClientRect(getDocumentRect(getDocumentElement(element)));
}
// A "clipping parent" is an overflowable container with the characteristic of
// clipping (or hiding) overflowing elements with a position different from
// `initial`
/**
 * @param element
 */
function getClippingParents(element) {
    const clippingParents = listScrollParents(getParentNode(element));
    const canEscapeClipping = ['absolute', 'fixed'].indexOf(getComputedStyle(element).position) >= 0;
    const clipperElement = canEscapeClipping && isHTMLElement(element) ? getOffsetParent(element) : element;
    if (!isElement(clipperElement)) {
        return [];
    }
    // $FlowFixMe: https://github.com/facebook/flow/issues/1414
    return clippingParents.filter(clippingParent => isElement(clippingParent) && contains(clippingParent, clipperElement) && getNodeName(clippingParent) !== 'body');
}
// Gets the maximum area that the element is visible in due to any number of
// clipping parents
/**
 * @param element
 * @param boundary
 * @param rootBoundary
 */
export default function getClippingRect(element, boundary, rootBoundary) {
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
//# sourceMappingURL=getClippingRect.js.map