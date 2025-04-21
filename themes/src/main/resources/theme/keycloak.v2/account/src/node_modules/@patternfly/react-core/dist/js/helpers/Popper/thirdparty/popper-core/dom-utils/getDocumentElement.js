"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
// @ts-nocheck
const instanceOf_1 = require("./instanceOf");
/**
 * @param element
 */
function getDocumentElement(element) {
    // $FlowFixMe: assume body is always available
    return (instanceOf_1.isElement(element) ? element.ownerDocument : element.document).documentElement;
}
exports.default = getDocumentElement;
//# sourceMappingURL=getDocumentElement.js.map