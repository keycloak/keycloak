// @ts-nocheck
import { Window } from '../types';

/**
 * @param element
 */
export default function getNodeName(element: (Node | null | undefined) | Window): string | null | undefined {
  return element ? (element.nodeName || '').toLowerCase() : null;
}
