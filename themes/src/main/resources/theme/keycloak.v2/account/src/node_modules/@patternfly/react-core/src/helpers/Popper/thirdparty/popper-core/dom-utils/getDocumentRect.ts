// @ts-nocheck
import { Rect } from '../types';
import getDocumentElement from './getDocumentElement';
import getComputedStyle from './getComputedStyle';
import getWindowScrollBarX from './getWindowScrollBarX';
import getWindowScroll from './getWindowScroll';

// Gets the entire size of the scrollable document area, even extending outside
// of the `<html>` and `<body>` rect bounds if horizontally scrollable
/**
 * @param element
 */
export default function getDocumentRect(element: HTMLElement): Rect {
  const html = getDocumentElement(element);
  const winScroll = getWindowScroll(element);
  const body = element.ownerDocument.body;

  const width = Math.max(html.scrollWidth, html.clientWidth, body ? body.scrollWidth : 0, body ? body.clientWidth : 0);
  const height = Math.max(
    html.scrollHeight,
    html.clientHeight,
    body ? body.scrollHeight : 0,
    body ? body.clientHeight : 0
  );

  let x = -winScroll.scrollLeft + getWindowScrollBarX(element);
  const y = -winScroll.scrollTop;

  if (getComputedStyle(body || html).direction === 'rtl') {
    x += Math.max(html.clientWidth, body ? body.clientWidth : 0) - width;
  }

  return { width, height, x, y };
}
