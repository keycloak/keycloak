// @ts-nocheck
import getWindow from './getWindow';
/**
 * @param element
 */
export default function getComputedStyle(element) {
    return getWindow(element).getComputedStyle(element);
}
//# sourceMappingURL=getComputedStyle.js.map