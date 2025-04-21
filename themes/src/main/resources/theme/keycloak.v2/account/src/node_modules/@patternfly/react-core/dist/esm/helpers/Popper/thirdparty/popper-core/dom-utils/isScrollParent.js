// @ts-nocheck
import getComputedStyle from './getComputedStyle';
/**
 * @param element
 */
export default function isScrollParent(element) {
    // Firefox wants us to check `-x` and `-y` variations as well
    const { overflow, overflowX, overflowY } = getComputedStyle(element);
    return /auto|scroll|overlay|hidden/.test(overflow + overflowY + overflowX);
}
//# sourceMappingURL=isScrollParent.js.map