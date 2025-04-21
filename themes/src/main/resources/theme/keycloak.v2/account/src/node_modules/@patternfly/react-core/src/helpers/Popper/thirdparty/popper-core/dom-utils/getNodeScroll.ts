// @ts-nocheck
import getWindowScroll from './getWindowScroll';
import getWindow from './getWindow';
import { isHTMLElement } from './instanceOf';
import getHTMLElementScroll from './getHTMLElementScroll';
import { Window } from '../types';

/**
 * @param node
 */
export default function getNodeScroll(node: Node | Window) {
  if (node === getWindow(node) || !isHTMLElement(node)) {
    return getWindowScroll(node);
  } else {
    return getHTMLElementScroll(node);
  }
}
