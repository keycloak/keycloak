// @ts-nocheck
import getNodeName from './getNodeName';
import getDocumentElement from './getDocumentElement';
/**
 * @param element
 */
export default function getParentNode(element) {
    if (getNodeName(element) === 'html') {
        return element;
    }
    return (
    // $FlowFixMe: this is a quicker (but less type safe) way to save quite some bytes from the bundle
    element.assignedSlot || // step into the shadow DOM of the parent of a slotted node
        element.parentNode || // DOM Element detected
        // $FlowFixMe: need a better way to handle this...
        element.host || // ShadowRoot detected
        // $FlowFixMe: HTMLElement is a Node
        getDocumentElement(element) // fallback
    );
}
//# sourceMappingURL=getParentNode.js.map