"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
// @ts-nocheck
const getNodeName_1 = tslib_1.__importDefault(require("./getNodeName"));
/**
 * @param element
 */
function isTableElement(element) {
    return ['table', 'td', 'th'].indexOf(getNodeName_1.default(element)) >= 0;
}
exports.default = isTableElement;
//# sourceMappingURL=isTableElement.js.map