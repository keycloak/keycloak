// @ts-nocheck
import getWindow from './getWindow';
import { Window } from '../types';

/**
 * @param node
 */
export default function getWindowScroll(node: Node | Window) {
  const win = getWindow(node);
  const scrollLeft = win.pageXOffset;
  const scrollTop = win.pageYOffset;

  return {
    scrollLeft,
    scrollTop
  };
}
