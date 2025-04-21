// @ts-nocheck
import getParentNode from './getParentNode';
import isScrollParent from './isScrollParent';
import getNodeName from './getNodeName';
import { isHTMLElement } from './instanceOf';

/**
 * @param node
 */
export default function getScrollParent(node: Node): HTMLElement {
  if (['html', 'body', '#document'].indexOf(getNodeName(node)) >= 0) {
    // $FlowFixMe: assume body is always available
    return node.ownerDocument.body;
  }

  if (isHTMLElement(node) && isScrollParent(node)) {
    return node;
  }

  return getScrollParent(getParentNode(node));
}
