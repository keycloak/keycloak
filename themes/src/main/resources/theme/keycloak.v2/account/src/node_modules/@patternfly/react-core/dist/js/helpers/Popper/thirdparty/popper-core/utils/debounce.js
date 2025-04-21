"use strict";
// @ts-nocheck
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @param fn
 */
function debounce(fn) {
    let pending;
    return () => {
        if (!pending) {
            pending = new Promise(resolve => {
                Promise.resolve().then(() => {
                    pending = undefined;
                    resolve(fn());
                });
            });
        }
        return pending;
    };
}
exports.default = debounce;
//# sourceMappingURL=debounce.js.map