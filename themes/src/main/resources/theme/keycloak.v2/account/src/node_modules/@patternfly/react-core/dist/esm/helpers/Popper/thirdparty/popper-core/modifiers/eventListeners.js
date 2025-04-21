import getWindow from '../dom-utils/getWindow';
const passive = { passive: true };
/**
 *
 */
function effect({ state, instance, options }) {
    const { scroll = true, resize = true } = options;
    const window = getWindow(state.elements.popper);
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
export default {
    name: 'eventListeners',
    enabled: true,
    phase: 'write',
    fn: () => { },
    effect,
    data: {}
};
//# sourceMappingURL=eventListeners.js.map