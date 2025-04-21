"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
// @ts-nocheck
const getParentNode_1 = tslib_1.__importDefault(require("./getParentNode"));
const isScrollParent_1 = tslib_1.__importDefault(require("./isScrollParent"));
const getNodeName_1 = tslib_1.__importDefault(require("./getNodeName"));
const instanceOf_1 = require("./instanceOf");
/**
 * @param node
 */
function getScrollParent(node) {
    if (['html', 'body', '#document'].indexOf(getNodeName_1.default(node)) >= 0) {
        // $FlowFixMe: assume body is always available
        return node.ownerDocument.body;
    }
    if (instanceOf_1.isHTMLElement(node) && isScrollParent_1.default(node)) {
        return node;
    }
    return getScrollParent(getParentNode_1.default(node));
}
exports.default = getScrollParent;
//# sourceMappingURL=getScrollParent.js.map