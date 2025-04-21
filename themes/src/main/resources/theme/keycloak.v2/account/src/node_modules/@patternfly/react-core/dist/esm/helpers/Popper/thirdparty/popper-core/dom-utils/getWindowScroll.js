// @ts-nocheck
import getWindow from './getWindow';
/**
 * @param node
 */
export default function getWindowScroll(node) {
    const win = getWindow(node);
    const scrollLeft = win.pageXOffset;
    const scrollTop = win.pageYOffset;
    return {
        scrollLeft,
        scrollTop
    };
}
//# sourceMappingURL=getWindowScroll.js.map