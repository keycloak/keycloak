// @ts-nocheck
import { Rect, ClientRectObject } from '../types';

/**
 * @param rect
 */
export default function rectToClientRect(rect: Rect): ClientRectObject {
  return {
    ...rect,
    left: rect.x,
    top: rect.y,
    right: rect.x + rect.width,
    bottom: rect.y + rect.height
  };
}
