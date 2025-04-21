// @ts-nocheck
import getWindow from './getWindow';

/* :: declare function isElement(node: mixed): boolean %checks(node instanceof
  Element); */
/**
 * @param node
 */
function isElement(node) {
  const OwnElement = getWindow(node).Element;
  return node instanceof OwnElement || node instanceof Element;
}

/* :: declare function isHTMLElement(node: mixed): boolean %checks(node instanceof
  HTMLElement); */
/**
 * @param node
 */
function isHTMLElement(node) {
  const OwnElement = getWindow(node).HTMLElement;
  return node instanceof OwnElement || node instanceof HTMLElement;
}

export { isElement, isHTMLElement };
