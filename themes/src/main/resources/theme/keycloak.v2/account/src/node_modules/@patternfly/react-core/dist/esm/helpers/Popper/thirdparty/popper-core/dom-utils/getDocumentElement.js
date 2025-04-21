// @ts-nocheck
import { isElement } from './instanceOf';
/**
 * @param element
 */
export default function getDocumentElement(element) {
    // $FlowFixMe: assume body is always available
    return (isElement(element) ? element.ownerDocument : element.document).documentElement;
}
//# sourceMappingURL=getDocumentElement.js.map