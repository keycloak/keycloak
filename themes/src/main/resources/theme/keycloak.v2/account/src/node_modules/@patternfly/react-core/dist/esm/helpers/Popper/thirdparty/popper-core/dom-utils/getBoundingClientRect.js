/**
 * @param element
 */
export default function getBoundingClientRect(element) {
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
//# sourceMappingURL=getBoundingClientRect.js.map