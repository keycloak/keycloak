/**
 * @param rect
 */
export default function rectToClientRect(rect) {
    return Object.assign(Object.assign({}, rect), { left: rect.x, top: rect.y, right: rect.x + rect.width, bottom: rect.y + rect.height });
}
//# sourceMappingURL=rectToClientRect.js.map