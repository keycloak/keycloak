"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
// @ts-nocheck
const getWindowScroll_1 = tslib_1.__importDefault(require("./getWindowScroll"));
const getWindow_1 = tslib_1.__importDefault(require("./getWindow"));
const instanceOf_1 = require("./instanceOf");
const getHTMLElementScroll_1 = tslib_1.__importDefault(require("./getHTMLElementScroll"));
/**
 * @param node
 */
function getNodeScroll(node) {
    if (node === getWindow_1.default(node) || !instanceOf_1.isHTMLElement(node)) {
        return getWindowScroll_1.default(node);
    }
    else {
        return getHTMLElementScroll_1.default(node);
    }
}
exports.default = getNodeScroll;
//# sourceMappingURL=getNodeScroll.js.map