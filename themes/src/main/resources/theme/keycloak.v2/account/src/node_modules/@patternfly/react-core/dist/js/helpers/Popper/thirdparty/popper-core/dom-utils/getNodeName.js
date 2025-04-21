"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @param element
 */
function getNodeName(element) {
    return element ? (element.nodeName || '').toLowerCase() : null;
}
exports.default = getNodeName;
//# sourceMappingURL=getNodeName.js.map