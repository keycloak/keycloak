"use strict";
// @ts-nocheck
Object.defineProperty(exports, "__esModule", { value: true });
/* :: import type { Window } from '../types'; */
/* :: declare function getWindow(node: Node | Window): Window; */
/**
 * @param node
 */
function getWindow(node) {
    if (node.toString() !== '[object Window]') {
        const ownerDocument = node.ownerDocument;
        return ownerDocument ? ownerDocument.defaultView : window;
    }
    return node;
}
exports.default = getWindow;
//# sourceMappingURL=getWindow.js.map