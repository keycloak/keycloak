// @ts-nocheck

/* :: import type { Window } from '../types'; */

/* :: declare function getWindow(node: Node | Window): Window; */

/**
 * @param node
 */
export default function getWindow(node) {
  if (node.toString() !== '[object Window]') {
    const ownerDocument = node.ownerDocument;
    return ownerDocument ? ownerDocument.defaultView : window;
  }

  return node;
}
