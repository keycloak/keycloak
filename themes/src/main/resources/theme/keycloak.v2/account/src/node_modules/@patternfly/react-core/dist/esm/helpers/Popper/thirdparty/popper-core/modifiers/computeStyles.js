import { top, left, right, bottom } from '../enums';
import getOffsetParent from '../dom-utils/getOffsetParent';
import getWindow from '../dom-utils/getWindow';
import getDocumentElement from '../dom-utils/getDocumentElement';
import getComputedStyle from '../dom-utils/getComputedStyle';
import getBasePlacement from '../utils/getBasePlacement';
const unsetSides = {
    top: 'auto',
    right: 'auto',
    bottom: 'auto',
    left: 'auto'
};
// Round the offsets to the nearest suitable subpixel based on the DPR.
// Zooming can change the DPR, but it seems to report a value that will
// cleanly divide the values into the appropriate subpixels.
/**
 *
 */
function roundOffsets({ x, y }) {
    const win = window;
    const dpr = win.devicePixelRatio || 1;
    return {
        x: Math.round(x * dpr) / dpr || 0,
        y: Math.round(y * dpr) / dpr || 0
    };
}
/**
 *
 */
export function mapToStyles({ popper, popperRect, placement, offsets, position, gpuAcceleration, adaptive }) {
    let { x, y } = roundOffsets(offsets);
    const hasX = offsets.hasOwnProperty('x');
    const hasY = offsets.hasOwnProperty('y');
    let sideX = left;
    let sideY = top;
    const win = window;
    if (adaptive) {
        let offsetParent = getOffsetParent(popper);
        if (offsetParent === getWindow(popper)) {
            offsetParent = getDocumentElement(popper);
        }
        // $FlowFixMe: force type refinement, we compare offsetParent with window above, but Flow doesn't detect it
        /* :: offsetParent = (offsetParent: Element); */
        if (placement === top) {
            sideY = bottom;
            y -= offsetParent.clientHeight - popperRect.height;
            y *= gpuAcceleration ? 1 : -1;
        }
        if (placement === left) {
            sideX = right;
            x -= offsetParent.clientWidth - popperRect.width;
            x *= gpuAcceleration ? 1 : -1;
        }
    }
    const commonStyles = Object.assign({ position }, (adaptive && unsetSides));
    if (gpuAcceleration) {
        return Object.assign(Object.assign({}, commonStyles), { [sideY]: hasY ? '0' : '', [sideX]: hasX ? '0' : '', 
            // Layer acceleration can disable subpixel rendering which causes slightly
            // blurry text on low PPI displays, so we want to use 2D transforms
            // instead
            transform: (win.devicePixelRatio || 1) < 2 ? `translate(${x}px, ${y}px)` : `translate3d(${x}px, ${y}px, 0)` });
    }
    return Object.assign(Object.assign({}, commonStyles), { [sideY]: hasY ? `${y}px` : '', [sideX]: hasX ? `${x}px` : '', transform: '' });
}
/**
 *
 */
function computeStyles({ state, options }) {
    const { gpuAcceleration = true, adaptive = true } = options;
    if (false /* __DEV__*/) {
        const transitionProperty = getComputedStyle(state.elements.popper).transitionProperty || '';
        if (adaptive &&
            ['transform', 'top', 'right', 'bottom', 'left'].some(property => transitionProperty.indexOf(property) >= 0)) {
            console.warn([
                'Popper: Detected CSS transitions on at least one of the following',
                'CSS properties: "transform", "top", "right", "bottom", "left".',
                '\n\n',
                'Disable the "computeStyles" modifier\'s `adaptive` option to allow',
                'for smooth transitions, or remove these properties from the CSS',
                'transition declaration on the popper element if only transitioning',
                'opacity or background-color for example.',
                '\n\n',
                'We recommend using the popper element as a wrapper around an inner',
                'element that can have any CSS property transitioned for animations.'
            ].join(' '));
        }
    }
    const commonStyles = {
        placement: getBasePlacement(state.placement),
        popper: state.elements.popper,
        popperRect: state.rects.popper,
        gpuAcceleration
    };
    if (state.modifiersData.popperOffsets != null) {
        state.styles.popper = Object.assign(Object.assign({}, state.styles.popper), mapToStyles(Object.assign(Object.assign({}, commonStyles), { offsets: state.modifiersData.popperOffsets, position: state.options.strategy, adaptive })));
    }
    if (state.modifiersData.arrow != null) {
        state.styles.arrow = Object.assign(Object.assign({}, state.styles.arrow), mapToStyles(Object.assign(Object.assign({}, commonStyles), { offsets: state.modifiersData.arrow, position: 'absolute', adaptive: false })));
    }
    state.attributes.popper = Object.assign(Object.assign({}, state.attributes.popper), { 'data-popper-placement': state.placement });
}
export default {
    name: 'computeStyles',
    enabled: true,
    phase: 'beforeWrite',
    fn: computeStyles,
    data: {}
};
//# sourceMappingURL=computeStyles.js.map