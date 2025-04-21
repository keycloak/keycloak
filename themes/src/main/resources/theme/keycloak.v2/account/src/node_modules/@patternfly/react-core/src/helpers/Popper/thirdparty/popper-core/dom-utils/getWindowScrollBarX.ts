// @ts-nocheck
import getBoundingClientRect from './getBoundingClientRect';
import getDocumentElement from './getDocumentElement';
import getWindowScroll from './getWindowScroll';

/**
 * @param element
 */
export default function getWindowScrollBarX(element: Element): number {
  // If <html> has a CSS width greater than the viewport, then this will be
  // incorrect for RTL.
  // Popper 1 is broken in this case and never had a bug report so let's assume
  // it's not an issue. I don't think anyone ever specifies width on <html>
  // anyway.
  // Browsers where the left scrollbar doesn't cause an issue report `0` for
  // this (e.g. Edge 2019, IE11, Safari)
  return getBoundingClientRect(getDocumentElement(element)).left + getWindowScroll(element).scrollLeft;
}
