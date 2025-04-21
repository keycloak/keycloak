"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.isHTMLElement = exports.isElement = void 0;
const tslib_1 = require("tslib");
// @ts-nocheck
const getWindow_1 = tslib_1.__importDefault(require("./getWindow"));
/* :: declare function isElement(node: mixed): boolean %checks(node instanceof
  Element); */
/**
 * @param node
 */
function isElement(node) {
    const OwnElement = getWindow_1.default(node).Element;
    return node instanceof OwnElement || node instanceof Element;
}
exports.isElement = isElement;
/* :: declare function isHTMLElement(node: mixed): boolean %checks(node instanceof
  HTMLElement); */
/**
 * @param node
 */
function isHTMLElement(node) {
    const OwnElement = getWindow_1.default(node).HTMLElement;
    return node instanceof OwnElement || node instanceof HTMLElement;
}
exports.isHTMLElement = isHTMLElement;
//# sourceMappingURL=instanceOf.js.map