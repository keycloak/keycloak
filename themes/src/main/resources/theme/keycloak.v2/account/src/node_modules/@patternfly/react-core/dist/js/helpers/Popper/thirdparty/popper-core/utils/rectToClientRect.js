"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @param rect
 */
function rectToClientRect(rect) {
    return Object.assign(Object.assign({}, rect), { left: rect.x, top: rect.y, right: rect.x + rect.width, bottom: rect.y + rect.height });
}
exports.default = rectToClientRect;
//# sourceMappingURL=rectToClientRect.js.map