// @ts-nocheck
import getWindowScroll from './getWindowScroll';
import getWindow from './getWindow';
import { isHTMLElement } from './instanceOf';
import getHTMLElementScroll from './getHTMLElementScroll';
/**
 * @param node
 */
export default function getNodeScroll(node) {
    if (node === getWindow(node) || !isHTMLElement(node)) {
        return getWindowScroll(node);
    }
    else {
        return getHTMLElementScroll(node);
    }
}
//# sourceMappingURL=getNodeScroll.js.map