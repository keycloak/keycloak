// @ts-nocheck
import { isElement } from './instanceOf';
import { Window } from '../types';

/**
 * @param element
 */
export default function getDocumentElement(element: Element | Window): HTMLElement {
  // $FlowFixMe: assume body is always available
  return (isElement(element) ? element.ownerDocument : element.document).documentElement;
}
