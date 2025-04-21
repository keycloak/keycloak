"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @param element
 */
function getBoundingClientRect(element) {
    const rect = element.getBoundingClientRect();
    return {
        width: rect.width,
        height: rect.height,
        top: rect.top,
        right: rect.right,
        bottom: rect.bottom,
        left: rect.left,
        x: rect.left,
        y: rect.top
    };
}
exports.default = getBoundingClientRect;
//# sourceMappingURL=getBoundingClientRect.js.map