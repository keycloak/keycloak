"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
// @ts-nocheck
const getWindow_1 = tslib_1.__importDefault(require("./getWindow"));
const getDocumentElement_1 = tslib_1.__importDefault(require("./getDocumentElement"));
const getWindowScrollBarX_1 = tslib_1.__importDefault(require("./getWindowScrollBarX"));
/**
 * @param element
 */
function getViewportRect(element) {
    const win = getWindow_1.default(element);
    const html = getDocumentElement_1.default(element);
    const visualViewport = win.visualViewport;
    let width = html.clientWidth;
    let height = html.clientHeight;
    let x = 0;
    let y = 0;
    // NB: This isn't supported on iOS <= 12. If the keyboard is open, the popper
    // can be obscured underneath it.
    // Also, `html.clientHeight` adds the bottom bar height in Safari iOS, even
    // if it isn't open, so if this isn't available, the popper will be detected
    // to overflow the bottom of the screen too early.
    if (visualViewport) {
        width = visualViewport.width;
        height = visualViewport.height;
        // Uses Layout Viewport (like Chrome; Safari does not currently)
        // In Chrome, it returns a value very close to 0 (+/-) but contains rounding
        // errors due to floating point numbers, so we need to check precision.
        // Safari returns a number <= 0, usually < -1 when pinch-zoomed
        // Feature detection fails in mobile emulation mode in Chrome.
        // Math.abs(win.innerWidth / visualViewport.scale - visualViewport.width) <
        // 0.001
        // Fallback here: "Not Safari" userAgent
        if (!/^((?!chrome|android).)*safari/i.test(navigator.userAgent)) {
            x = visualViewport.offsetLeft;
            y = visualViewport.offsetTop;
        }
    }
    return {
        width,
        height,
        x: x + getWindowScrollBarX_1.default(element),
        y
    };
}
exports.default = getViewportRect;
//# sourceMappingURL=getViewportRect.js.map