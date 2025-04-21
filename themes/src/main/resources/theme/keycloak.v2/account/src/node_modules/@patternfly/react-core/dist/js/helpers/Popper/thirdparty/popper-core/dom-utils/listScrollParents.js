"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
// @ts-nocheck
const getScrollParent_1 = tslib_1.__importDefault(require("./getScrollParent"));
const getParentNode_1 = tslib_1.__importDefault(require("./getParentNode"));
const getNodeName_1 = tslib_1.__importDefault(require("./getNodeName"));
const getWindow_1 = tslib_1.__importDefault(require("./getWindow"));
const isScrollParent_1 = tslib_1.__importDefault(require("./isScrollParent"));
/*
given a DOM element, return the list of all scroll parents, up the list of ancesors
until we get to the top window object. This list is what we attach scroll listeners
to, because if any of these parent elements scroll, we'll need to re-calculate the
reference element's position.
*/
/**
 * @param element
 * @param list
 */
function listScrollParents(element, list = []) {
    const scrollParent = getScrollParent_1.default(element);
    const isBody = getNodeName_1.default(scrollParent) === 'body';
    const win = getWindow_1.default(scrollParent);
    const target = isBody
        ? [win].concat(win.visualViewport || [], isScrollParent_1.default(scrollParent) ? scrollParent : [])
        : scrollParent;
    const updatedList = list.concat(target);
    return isBody
        ? updatedList // $FlowFixMe: isBody tells us target will be an HTMLElement here
        : updatedList.concat(listScrollParents(getParentNode_1.default(target)));
}
exports.default = listScrollParents;
//# sourceMappingURL=listScrollParents.js.map