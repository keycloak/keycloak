"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
// Returns the layout rect of an element relative to its offsetParent. Layout
// means it doesn't take into account transforms.
/**
 * @param element
 */
function getLayoutRect(element) {
    return {
        x: element.offsetLeft,
        y: element.offsetTop,
        width: element.offsetWidth,
        height: element.offsetHeight
    };
}
exports.default = getLayoutRect;
//# sourceMappingURL=getLayoutRect.js.map