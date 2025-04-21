"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
const getWindow_1 = tslib_1.__importDefault(require("../dom-utils/getWindow"));
const passive = { passive: true };
/**
 *
 */
function effect({ state, instance, options }) {
    const { scroll = true, resize = true } = options;
    const window = getWindow_1.default(state.elements.popper);
    const scrollParents = [...state.scrollParents.reference, ...state.scrollParents.popper];
    if (scroll) {
        scrollParents.forEach(scrollParent => {
            scrollParent.addEventListener('scroll', instance.update, passive);
        });
    }
    if (resize) {
        window.addEventListener('resize', instance.update, passive);
    }
    return () => {
        if (scroll) {
            scrollParents.forEach(scrollParent => {
                scrollParent.removeEventListener('scroll', instance.update, passive);
            });
        }
        if (resize) {
            window.removeEventListener('resize', instance.update, passive);
        }
    };
}
exports.default = {
    name: 'eventListeners',
    enabled: true,
    phase: 'write',
    fn: () => { },
    effect,
    data: {}
};
//# sourceMappingURL=eventListeners.js.map