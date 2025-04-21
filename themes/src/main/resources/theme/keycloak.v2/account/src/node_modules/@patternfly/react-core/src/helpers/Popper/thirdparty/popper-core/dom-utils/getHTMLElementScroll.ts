// @ts-nocheck

/**
 * @param element
 */
export default function getHTMLElementScroll(element: HTMLElement) {
  return {
    scrollLeft: element.scrollLeft,
    scrollTop: element.scrollTop
  };
}
