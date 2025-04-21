// @ts-nocheck
import getWindow from './getWindow';

/**
 * @param element
 */
export default function getComputedStyle(element: Element): CSSStyleDeclaration {
  return getWindow(element).getComputedStyle(element);
}
