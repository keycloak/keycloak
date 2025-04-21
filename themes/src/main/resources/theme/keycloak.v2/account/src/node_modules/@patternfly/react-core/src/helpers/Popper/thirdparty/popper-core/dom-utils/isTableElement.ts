// @ts-nocheck
import getNodeName from './getNodeName';

/**
 * @param element
 */
export default function isTableElement(element: Element): boolean {
  return ['table', 'td', 'th'].indexOf(getNodeName(element)) >= 0;
}
