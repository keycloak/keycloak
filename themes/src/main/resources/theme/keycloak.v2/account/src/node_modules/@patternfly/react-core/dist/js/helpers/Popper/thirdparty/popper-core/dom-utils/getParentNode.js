"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
// @ts-nocheck
const getNodeName_1 = tslib_1.__importDefault(require("./getNodeName"));
const getDocumentElement_1 = tslib_1.__importDefault(require("./getDocumentElement"));
/**
 * @param element
 */
function getParentNode(element) {
    if (getNodeName_1.default(element) === 'html') {
        return element;
    }
    return (
    // $FlowFixMe: this is a quicker (but less type safe) way to save quite some bytes from the bundle
    element.assignedSlot || // step into the shadow DOM of the parent of a slotted node
        element.parentNode || // DOM Element detected
        // $FlowFixMe: need a better way to handle this...
        element.host || // ShadowRoot detected
        // $FlowFixMe: HTMLElement is a Node
        getDocumentElement_1.default(element) // fallback
    );
}
exports.default = getParentNode;
//# sourceMappingURL=getParentNode.js.map