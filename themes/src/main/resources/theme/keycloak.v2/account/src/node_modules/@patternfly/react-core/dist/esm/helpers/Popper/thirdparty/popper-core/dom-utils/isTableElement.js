// @ts-nocheck
import getNodeName from './getNodeName';
/**
 * @param element
 */
export default function isTableElement(element) {
    return ['table', 'td', 'th'].indexOf(getNodeName(element)) >= 0;
}
//# sourceMappingURL=isTableElement.js.map